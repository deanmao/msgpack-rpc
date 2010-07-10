#
# MessagePack-RPC for Ruby TCP transport
#
#    Licensed under the Apache License, Version 2.0 (the "License");
#    you may not use this file except in compliance with the License.
#    You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#    Unless required by applicable law or agreed to in writing, software
#    distributed under the License is distributed on an "AS IS" BASIS,
#    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#    See the License for the specific language governing permissions and
#    limitations under the License.
#

# Alpha version of redis transport, great for durable & fast rpc
# 
# Here's a quick example:
# 
# Client:
# require "msgpack/rpc"
# require 'msgpack/rpc/transport/redis'
# 
# client = MessagePack::RPC::Client.new(MessagePack::RPC::RedisTransport.new("my_queue_name"), nil)
# result = client.call(:mymethod, "hello", 1)
#
# 
# Server:
# require "msgpack/rpc"
# require 'msgpack/rpc/transport/redis'
# 
# class ServerImpl
#   def mymethod(string, number)
#     puts "mymethod() got called... #{string.inspect} #{number.inspect}"
#     puts "I'm now returning the string 'hello'"
#     "hello"
#   end
# end
# 
# svr = MessagePack::RPC::Server.new
# svr.listen(MessagePack::RPC::RedisServerTransport.new('my_queue_name'), ServerImpl.new)

require 'redis'

module MessagePack
module RPC

REDIS_DATA = 535 # == 'redis'.split('').inject(0) {|sum,x| sum+=x.ord} -- I know this is lame, but had to think of something!

class RedisTransport
  def initialize(name, redis_options = {})
    @queue_name = "msgpack_queue:#{name}"
    @redis_options = redis_options
  end

	def build_transport(session, address)
		RedisClientTransport.new(session, @redis_options, @queue_name)
	end
end


class RedisClientTransport
  attr_reader :identifier
  
	def initialize(session, redis_options, queue_name)
		@session = session
		@queue_name = queue_name
		@redis = Redis.new(redis_options)
		# assign a unique client key for this instance which will be used for return values
		@identifier = @redis.incr("MessagePack-RPC-Client-Key")
		@return_queue_name = "rpc-return-identifier-#{@identifier}"
		@checker = MessagePack::RPC::LoopUtil::Timer.new(1, true) do |x|
  		data = @redis.blpop(@return_queue_name, 0)
      if data[1]
        redis_packet = MessagePack.unpack(data[1])
        if redis_packet[1] == REDIS_DATA
          msg = MessagePack.unpack(redis_packet[0])
          if msg[0] == RESPONSE
            on_response(msg[1], msg[2], msg[3])
          else
            puts "unknown message type #{msg[0]}"
          end
        end
      end
	  end
		@session.loop.attach(@checker)
	end

	def send_data(data)
    @redis.rpush(@queue_name, [data, REDIS_DATA, @return_queue_name].to_msgpack)
	end

	def close
		self
	end

	def on_connect(sock)
	end

	def on_response(msgid, error, result)
		@session.on_response(self, msgid, error, result)
	end
	
	def on_connect_failed(sock)
	end

	def on_close(sock)
	end
end

class RedisServerTransport
	def initialize(name, redis_options = {})
		@redis = Redis.new(redis_options)
		@queue_name = "msgpack_queue:#{name}"
	end

	# ServerTransport interface
	def listen(server)
		@server = server
    loop do
      begin
    		data = @redis.blpop(@queue_name, 0)
    		if data[1]
    		  redis_packet = MessagePack.unpack(data[1])
    		  if redis_packet[1] == REDIS_DATA
    		    @identifier = redis_packet[2]
    		    msg = MessagePack.unpack(redis_packet[0])
      		  case msg[0]
        		when REQUEST
        			@server.on_request(self, msg[1], msg[2], msg[3])
        		when RESPONSE
        		  puts "response message on server session"
        		when NOTIFY
        			@server.on_notify(msg[1], msg[2])
        		else
        			puts "unknown message type #{msg[0]}"
        		end
  		    end
    		end
    	rescue => e
    	  puts "probably timed out..."
		  end
    end
	end
	
	def send_data(data)
    @redis.rpush(@identifier, [data, REDIS_DATA].to_msgpack) if @identifier
  end

	# ServerTransport interface
	def close
	end
end

end
end

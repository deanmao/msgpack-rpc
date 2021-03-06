//
// MessagePack-RPC for Java
//
// Copyright (C) 2010 Kazuki Ohta
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.msgpack.rpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.*;

import org.junit.Test;
import org.msgpack.rpc.client.Client;
import org.msgpack.rpc.client.EventLoop;
import org.msgpack.rpc.client.TCPClient;
import org.msgpack.rpc.client.UDPClient;
import org.msgpack.rpc.server.TCPServer;
import org.msgpack.rpc.server.UDPServer;

public class ServerTest extends TestCase {
    @Test    
    public void testTCP() throws Exception{
        EventLoop loop = new EventLoop();
        try {
            ServerMock s = new ServerMock(new TCPServer("0.0.0.0", 19850, this));
            Client c = new TCPClient("localhost", 19850, loop);
            try {
                testRPC(c, s);
            } finally {
                c.close();
            }
        } finally {
            loop.shutdown();
        }
    }
    
    @Test
    public void testUDP() throws Exception{
        EventLoop loop = new EventLoop();
        try {
            ServerMock s = new ServerMock(new UDPServer("0.0.0.0", 19850, this));
            Client c = new UDPClient("localhost", 19850, loop);
            try {
                testRPC(c, s);
            } finally {
                c.close();
            }
        } finally {
            loop.shutdown();
        }
    }
    
    protected void testRPC(Client c, ServerMock sm) throws Exception {       
        sm.startServer();
        try {
            testInt(c);
            testFloat(c);
            testDouble(c);
            testNil(c);
            testBool(c);
            testString(c);
            testArray(c);
            testMap(c);
            c.close();
        } finally {
            sm.stopServer();
        }
    }
    
    public int intFunc0() { return 0; }
    public int intFunc1(int a) { return a; }
    public int intFunc2(int a, int b) { return b; }
    protected void testInt(Client c) throws Exception {
        Object o;
        o = c.call("intFunc0");
        assertEquals(0, ((Number)o).intValue());
        o = c.call("intFunc1", 1);
        assertEquals(1, ((Number)o).intValue());
        o = c.call("intFunc2", 1, 2);
        assertEquals(2, ((Number)o).intValue());
    }
    
    public float floatFunc0() { return (float)0.0; }
    public float floatFunc1(Float a) { return a; }
    public float floatFunc2(Float a, Float b) { return b; }
    protected void testFloat(Client c) throws Exception {
        Object o;
        o = c.call("floatFunc0");
        assertEquals(0.0, ((Float)o).floatValue(), 10e-10);
        o = c.call("floatFunc1", (float)1.0);
        assertEquals(1.0, ((Float)o).floatValue(), 10e-10);
        o = c.call("floatFunc2", (float)1.0, (float)2.0);
        assertEquals(2.0, ((Float)o).floatValue(), 10e-10);
    }
    
    public double doubleFunc0() { return 0.0; }
    public double doubleFunc1(Double a) { return a; }
    public double doubleFunc2(Double a, Double b) { return b; }
    protected void testDouble(Client c) throws Exception {
        Object o;
        o = c.call("doubleFunc0");
        assertEquals(0.0, ((Double)o).doubleValue(), 10e-10);
        o = c.call("doubleFunc1", 1.0);
        assertEquals(1.0, ((Double)o).doubleValue(), 10e-10);
        o = c.call("doubleFunc2", 1.0, 2.0);
        assertEquals(2.0, ((Double)o).doubleValue(), 10e-10);
    }
    
    public Object nilFunc0() { return null; }
    public Object nilFunc1(Object a) { return a; }
    public Object nilFunc2(Object a, Object b) { return b; }
    protected void testNil(Client c) throws Exception {
        Object o;
        o = c.call("nilFunc0");
        assertEquals(null, o);
        o = c.call("nilFunc1", null);
        assertEquals(null, o);
        o = c.call("nilFunc2", null, null);
        assertEquals(null, o);
    }
    
    public Boolean boolFunc0() { return false; }
    public Boolean boolFunc1(Boolean a) { return a; }
    public Boolean boolFunc2(Boolean a, Boolean b) { return b; }
    protected void testBool(Client c) throws Exception {
        Object o;
        o = c.call("boolFunc0");
        assertEquals(false, ((Boolean)o).booleanValue());
        o = c.call("boolFunc1", false);
        assertEquals(false, ((Boolean)o).booleanValue());
        o = c.call("boolFunc2", false, true);
        assertEquals(true,  ((Boolean)o).booleanValue());
    }
    
    public String strFunc0() { return "0"; }
    public String strFunc1(byte[] a) { return new String(a); }
    public String strFunc2(byte[] a, byte[] b) { return new String(b); }
    protected void testString(Client c) throws Exception {
        Object o;
        o = c.call("strFunc0");
        assertEquals("0", new String((byte[])o));
        o = c.call("strFunc1", "1");
        assertEquals("1", new String((byte[])o));
        o = c.call("strFunc2", "1", "2");
        assertEquals("2", new String((byte[])o));
    }

    public List<Byte> arrayFunc0() { return new ArrayList<Byte>(); }
    public List<Byte> arrayFunc1(List<Byte> a) { return a; }
    public List<Byte> arrayFunc2(List<Byte> a, List<Byte> b) { return b; }
    protected void testArray(Client c) throws Exception {
        Object o;
        o = c.call("arrayFunc0");
        assertEquals(new ArrayList<Byte>(), o);
        
        List<Byte> a1 = new ArrayList<Byte>();
        a1.add((byte)1);
        a1.add((byte)2);
        a1.add((byte)3);
        o = c.call("arrayFunc1", a1);
        assertEquals(a1, o);
        
        List<Byte> a2 = new ArrayList<Byte>();
        a2.add((byte)11);
        a2.add((byte)12);
        a2.add((byte)13);
        o = c.call("arrayFunc2", a1, a2);
        assertEquals(a2, o);
    }

    public Map<Byte, Byte> mapFunc0() { return new HashMap<Byte, Byte>(); }
    public Map<Byte, Byte> mapFunc1(Map<Byte, Byte> a) { return a; }
    public Map<Byte, Byte> mapFunc2(Map<Byte, Byte> a, Map<Byte, Byte> b) { return b; }
    protected void testMap(Client c) throws Exception {
        Object o;
        o = c.call("mapFunc0");
        assertEquals(new HashMap<Byte, Byte>(), o);

        HashMap<Byte, Byte> m1 = new HashMap<Byte, Byte>();
        m1.put((byte)1, (byte)1);
        m1.put((byte)2, (byte)2);
        m1.put((byte)3, (byte)3);
        o = c.call("mapFunc1", m1);
        assertEquals(m1, o);

        HashMap<Byte, Byte> m2 = new HashMap<Byte, Byte>();
        m2.put((byte)11, (byte)11);
        m2.put((byte)12, (byte)12);
        m2.put((byte)13, (byte)13);
        o = c.call("mapFunc2", m1, m2);
        assertEquals(m2, o);
    }
}

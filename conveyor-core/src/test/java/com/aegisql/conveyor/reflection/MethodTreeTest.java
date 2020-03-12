package com.aegisql.conveyor.reflection;

import com.aegisql.conveyor.ConveyorRuntimeException;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.Assert.*;

public class MethodTreeTest {

    class X{
        @Label("a")
        @NoLabel
        void x(){}
    }

    void a(){}
    void a(String x){}
    void a(String x, String y){}
    @Label({"bee","bi"})
    void b(String x){}
    void o(CharSequence x){}
    void o(CharSequence x,CharSequence y){}
    void o(CharSequence x,CharSequence y,CharSequence z){}

    @NoLabel
    void noLabel(){};

    @Test
    public void basicTest() {
        MethodTree mt = new MethodTree();
        mt.addMethod(getMethod("a"));
        mt.addMethod(getMethod("a",new Class[]{String.class}));
        mt.addMethod(getMethod("b",new Class[]{String.class}));
        mt.addMethod(getMethod("o",new Class[]{CharSequence.class,CharSequence.class}));
        mt.addMethod(getMethod("o",new Class[]{CharSequence.class}));
        mt.addMethod(getMethod("a",new Class[]{String.class, String.class}));
        System.out.println(mt);

        Method a = mt.findMethod("a");
        System.out.println(a);
        assertNotNull(a);
        assertEquals("a",a.getName());
        assertEquals(0,a.getParameterCount());

        Method aS = mt.findMethod("a",new Class[]{String.class});
        System.out.println(aS);
        assertNotNull(aS);
        assertEquals("a",aS.getName());
        assertEquals(1,aS.getParameterCount());

        Method aSS = mt.findMethod("a",new Class[]{String.class,String.class});
        System.out.println(aSS);
        assertNotNull(aSS);
        assertEquals("a",aSS.getName());
        assertEquals(2,aSS.getParameterCount());

        Method bS = mt.findMethod("b",new Class[]{String.class});
        System.out.println(bS);
        assertNotNull(bS);
        assertEquals("b",bS.getName());
        assertEquals(1,bS.getParameterCount());
        assertEquals(bS,mt.findMethod("bee",new Class[]{String.class}));
        assertEquals(bS,mt.findMethod("bi",new Class[]{String.class}));



        Method oS = mt.findMethod("o",new Class[]{String.class});
        System.out.println(oS);
        assertNotNull(oS);
        assertEquals("o",oS.getName());
        assertEquals(1,oS.getParameterCount());

        Method oSS = mt.findMethod("o",new Class[]{String.class,String.class});
        System.out.println(oSS);
        assertNotNull(oSS);
        assertEquals("o",oSS.getName());
        assertEquals(2,oSS.getParameterCount());

        Set<Method> aSSL = mt.findMethodCandidates("a",new Class[]{String.class,null});
        System.out.println(aSSL);
        assertNotNull(aSSL);
        assertEquals(1, aSSL.size());
        Method aSS2 = aSSL.iterator().next();
        assertNotNull(aSS2);
        assertEquals(aSS,aSS2);

        Set<Method> oSN = mt.findMethodCandidates("o",new Class[]{null});
        System.out.println(oSN);
        assertNotNull(oSN);
        Method next = oSN.iterator().next();
        assertEquals("o",next.getName());
        assertEquals(1,next.getParameterCount());

        System.out.println(mt);
    }

    @Test
    public void testClassLookup() {
        MethodTree mt = new MethodTree(this);
        System.out.println(mt);
        Method a = mt.findMethod("a");
        System.out.println(a);
        assertNotNull(a);
        assertEquals("a",a.getName());
        assertEquals(0,a.getParameterCount());

        Method aS = mt.findMethod("a",new Class[]{String.class});
        System.out.println(aS);
        assertNotNull(aS);
        assertEquals("a",aS.getName());
        assertEquals(1,aS.getParameterCount());

        Method aSS = mt.findMethod("a",new Class[]{String.class,String.class});
        System.out.println(aSS);
        assertNotNull(aSS);
        assertEquals("a",aSS.getName());
        assertEquals(2,aSS.getParameterCount());

        Method bS = mt.findMethod("b",new Class[]{String.class});
        System.out.println(bS);
        assertNotNull(bS);
        assertEquals("b",bS.getName());
        assertEquals(1,bS.getParameterCount());

        Method oS = mt.findMethod("o",new Class[]{String.class});
        System.out.println(oS);
        assertNotNull(oS);
        assertEquals("o",oS.getName());
        assertEquals(1,oS.getParameterCount());

        Set<Method> aSSL = mt.findMethodCandidates("a",new Class[]{String.class,null});
        System.out.println(aSSL);
        assertNotNull(aSSL);
        assertEquals(1,aSSL.size());
        Method aSS2 = aSSL.iterator().next();
        assertNotNull(aSS2);
        assertEquals(aSS,aSS2);

        Set<Method> oSN = mt.findMethodCandidates("o",new Class[]{null});
        System.out.println(oSN);
        assertNotNull(oSN);
        Method next = oSN.iterator().next();
        assertEquals("o",next.getName());
        assertEquals(1,next.getParameterCount());

        Method nl = mt.findMethod("noLabel");
        assertNull(nl);

        Method n = mt.findMethod("z");
        assertNull(n);

        Set<Method> z = mt.findMethodCandidates("z",new Class[]{String.class,null});
        assertEquals(0,z.size());

    }

    @Test
    public void oCandidatesTest() {
        MethodTree mt = new MethodTree();
        mt.addMethod(getMethod("o",new Class[]{CharSequence.class,CharSequence.class}));
        Set<Method> oSSn = mt.findMethodCandidates("o",new Class[]{String.class,null});
        System.out.println(oSSn);
        assertNotNull(oSSn);
        assertEquals("o",oSSn.iterator().next().getName());

    }

    @Test
    public void findCandidatesForVoid() {
        MethodTree mt = new MethodTree(this);
        Set<Method> as = mt.findMethodCandidates("a");
        assertNotNull(as);
        assertEquals(1,as.size());
        Method next = as.iterator().next();
        assertEquals("a",next.getName());
        assertEquals(0,next.getParameterCount());
    }

    @Test
    public void ooCandidatesTest() {
        MethodTree mt = new MethodTree();
        mt.addMethod(getMethod("o",new Class[]{CharSequence.class,CharSequence.class,CharSequence.class}));
        Set<Method> oSSn = mt.findMethodCandidates("o",new Class[]{String.class,String.class,null});
        System.out.println(oSSn);
        assertNotNull(oSSn);
        assertEquals("o",oSSn.iterator().next().getName());
    }

    @Test
    public void oCandidatesTestFail() {
        MethodTree mt = new MethodTree();
        mt.addMethod(getMethod("o",new Class[]{CharSequence.class,CharSequence.class}));
        Set<Method> oSSn = mt.findMethodCandidates("o",new Class[]{Integer.class,null});
        assertEquals(0,oSSn.size());
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void ooCandidatesTestFail() {
        MethodTree mt = new MethodTree();
        mt.addMethod(getMethod("o",new Class[]{CharSequence.class,CharSequence.class,CharSequence.class}));
        Set<Method> oSSn = mt.findMethodCandidates("o",new Class[]{String.class,Integer.class,null});
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void classLookupShouldFailOnX(){
        MethodTree mt = new MethodTree(X.class);
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void classLookupShouldFailOnAssignableType(){
        MethodTree mt = new MethodTree(X.class);
        Method oI = mt.findMethod("o",new Class[]{Integer.class});
    }

    @Test(expected = ConveyorRuntimeException.class)
    public void classLookupShouldFailOnAssignableType2(){
        MethodTree mt = new MethodTree(X.class);
        Method oI = mt.findMethod("o",new Class[]{String.class,Integer.class});
    }

    Method getMethod(String name, Class<?>... args) {
        try {
            return this.getClass().getDeclaredMethod(name,args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


}
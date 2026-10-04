package dev.nexvisuals.core.visual;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RibbonMeshTest {
    @Test void newFadeCurveChangesOpacityWithoutChangingSweepGeometry() {
        var path=new TrailHistory(48);path.edges(0,0,0,-2,0,1,-2,.001,3);path.edges(10,1,0,-2,1,1,-2,.001,3);
        var original=RibbonMesh.sweep(path,40,100,-1,-1,2);
        var neutral=RibbonMesh.sweep(path,40,100,-1,-1,2,2);
        var soft=RibbonMesh.sweep(path,40,100,-1,-1,2,.75);
        assertEquals(original.vertices(),soft.vertices());
        for(int i=0;i<original.vertices();i++) {
            assertEquals(original.color(i),neutral.color(i));
            for(int axis=0;axis<3;axis++) assertEquals(original.coordinate(i,axis),soft.coordinate(i,axis));
        }
        assertTrue((soft.color(2)>>>24)>(original.color(2)>>>24));
    }
    private TrailHistory path(double origin) {
        TrailHistory history=new TrailHistory(64);
        for(int i=0;i<64;i++) history.point(i*50,origin+i*.1,1,origin,.01,3);
        return history;
    }
    private RibbonMesh mesh(TrailHistory h,boolean dual,int quality,double origin) {
        return RibbonMesh.player(h,3150,3200,.3,1,0xDD72DFFF,0x448C7FFF,quality,dual,true,
                origin,2,origin+3,origin+6.3,1,origin);
    }
    @Test void geometryBudgetIsFixedAndDualIsTwoDistinctBands() {
        TrailHistory path=path(0);
        RibbonMesh ribbon=mesh(path,false,3,0),dual=mesh(path,true,3,0);
        assertEquals(63*3*12,ribbon.vertices());assertEquals(2*ribbon.vertices(),dual.vertices());
        assertNotEquals(dual.coordinate(0,1),dual.coordinate(12,1));
        for(int i=0;i<dual.vertices();i++) for(int axis=0;axis<3;axis++) assertTrue(Float.isFinite(dual.coordinate(i,axis)));
    }
    @Test void cameraRelativeExtractionRetainsPrecisionAtWorldBorder() {
        RibbonMesh nearby=mesh(path(0),false,2,0),far=mesh(path(29_000_000),false,2,29_000_000);
        assertEquals(nearby.vertices(),far.vertices());
        for(int i=0;i<far.vertices();i++) for(int axis=0;axis<3;axis++) assertEquals(nearby.coordinate(i,axis),far.coordinate(i,axis),.00001);
    }
    @Test void tailFadesAndFullyExpiredHistoryProducesNoVertices() {
        TrailHistory path=path(0);RibbonMesh ribbon=mesh(path,false,1,0);
        assertTrue((ribbon.color(2)>>>24)<(ribbon.color(ribbon.vertices()-3)>>>24));
        assertEquals(0,ribbon.color(0)>>>24,"outer edge is soft");
        assertSame(RibbonMesh.EMPTY,RibbonMesh.player(path,7000,3200,.3,1,-1,-1,2,false,true,0,0,0,0,0,0));
    }
    @Test void bladeSweepUsesBothEdgesAndExpiresWithoutAnotherSwing() {
        TrailHistory path=new TrailHistory(48);
        path.edges(0,0,0,-2,0,1,-2,.001,3);path.edges(10,1,0,-2,1,1,-2,.001,3);
        RibbonMesh mesh=RibbonMesh.sweep(path,10,100,0xCC72DFFF,0x448C7FFF);
        assertEquals(12,mesh.vertices());assertEquals(1,mesh.coordinate(2,0));assertEquals(.15,mesh.coordinate(2,1),.00001);
        assertSame(RibbonMesh.EMPTY,RibbonMesh.sweep(path,110,100,-1,-1));
    }
    @Test void smoothedBladeMeshIsBoundedAndCannotOvershootItsProbes() {
        TrailHistory path=new TrailHistory(48);
        for(int i=0;i<48;i++) path.edges(i*10,Math.sin(i*.2)*.3,0,-1,Math.sin(i*.2)*.3,.4,-1,0,3);
        RibbonMesh mesh=RibbonMesh.sweep(path,470,500,0xCC72DFFF,0x448C7FFF,3);
        assertEquals(47*3*12,mesh.vertices());
        for(int i=0;i<mesh.vertices();i++) {
            assertTrue(mesh.coordinate(i,0)>=-.30001 && mesh.coordinate(i,0)<=.30001);
            assertTrue(mesh.coordinate(i,1)>=0 && mesh.coordinate(i,1)<=.40001);
            assertEquals(-1,mesh.coordinate(i,2));
        }
        assertEquals(0,mesh.color(0)>>>24,"outer edge stays transparent");
    }
}

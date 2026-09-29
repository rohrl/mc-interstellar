package io.github.rohrl.interstellar.wormhole;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpeningProgressTest {
    private static void advance(OpeningProgress p,double work,boolean image,int frames) {for(int i=0;i<frames;i++)p.advance(work,image,.02);}
    @Test void loneMouthNeverClaimsToBeOpening() {
        var p=new OpeningProgress();p.reset(false);advance(p,1,true,200);
        assertEquals(0,p.percent());assertEquals(.8,p.radius(8),1e-9);assertFalse(p.canReveal(true));assertFalse(p.open());
    }
    @Test void GeometryAloneCannotFinish() {
        var p=new OpeningProgress();p.reset(true);advance(p,1,false,200);
        assertEquals(95,p.percent());assertFalse(p.canReveal(true));
        advance(p,1,true,20);assertTrue(p.canReveal(true));assertFalse(p.canReveal(false));assertEquals(99,p.percent());
    }
    @Test void progressAndSizeNeverRegressWhenTheWindowExpands() {
        var p=new OpeningProgress();p.reset(true);advance(p,.8,true,100);int old=p.percent();double r=p.radius(8);
        advance(p,.2,true,100);assertEquals(old,p.percent());assertEquals(r,p.radius(8),1e-9);
    }
    @Test void actualPresentationAndRevealAreBothRequired() {
        var p=new OpeningProgress();p.reset(true);advance(p,1,true,100);p.beginReveal();
        advance(p,1,true,100);assertFalse(p.open());assertEquals(99,p.percent());
        p.presented();advance(p,1,true,5);assertFalse(p.open());
        advance(p,1,true,20);assertTrue(p.open());assertEquals(100,p.percent());assertEquals(0,p.previousImageOpacity());
        p.reset(true);assertFalse(p.open());assertFalse(p.revealing());assertEquals(0,p.percent());
    }
    @Test void pausesAndLongStallsDoNotSkipTheAnimation() {
        var p=new OpeningProgress();p.reset(true);p.advance(1,true,30);assertTrue(p.percent()<10);
        double radius=p.radius(8);p.advance(1,true,0);assertEquals(radius,p.radius(8),1e-9);
    }
}

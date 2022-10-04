import java.util.ArrayList;

import org.junit.Test;

import junit.framework.TestCase;
import util.Eleitor;

class PeerTest extends TestCase {

    Peer peer;

    public PeerTest(){
        peer = new Peer();
    }

    @Test
    public void testAddEleitor(){
        int cnt = 0;
        peer.addEleitor(new Eleitor(123, "teste"));
        peer.addEleitor(new Eleitor(123, "teste"));
        for(Eleitor e : peer.getEleitores()){
            if(e.getTitulo() == 123 || e.getNome().equals("teste")){
                cnt++;
            }
        }
        assertEquals(cnt, 1);
        peer.setEleitores(new ArrayList<>());
    } 

    /*public void testaPedCandidato(){
        int cnt = 0;
        peer.addEleitor(new Eleitor(123, "teste"));
        peer.addEleitor(new Eleitor(123, "teste"));
        for(Eleitor e : peer.getEleitores()){
            if(e.getTitulo() == 123 || e.getNome().equals("teste")){
                cnt++;
            }
        }
        assertEquals(cnt, 1);
        peer.setEleitores(new ArrayList<>());
    }*/
}
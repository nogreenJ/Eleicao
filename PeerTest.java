import org.junit.Test;

import junit.framework.TestCase;
import util.Candidato;
import util.Eleitor;
import util.Mensagem;

public class PeerTest extends TestCase {

    Peer peer;

    public PeerTest(){
        super();
        peer = new Peer();
    }

    //Cadastro de eleitores sem duplicações.
    @Test
    public void testAddEleitor(){
        int cnt = 0;
        peer.addEleitor(new Eleitor(321, "teste"));
        peer.addEleitor(new Eleitor(321, "teste"));
        for(Eleitor e : peer.getEleitores()){
            if(e.getTitulo() == 123 || e.getNome().equals("teste")){
                cnt++;
            }
        }
        assertEquals(cnt, 1);
    } 

    //Pedido de candidatura sem duplicações.
    @Test
    public void testaPedCandidato(){        
        int cnt = 0;
        Candidato cand = new Candidato(123, "teste", 321L);
        
        //Eleitor deve estar inscrito
        peer.addCandidato(cand, null, null);
        assertEquals(peer.getCandidatos().size(), 0);
        peer.addEleitor(new Eleitor(321, "teste"));
        peer.getEleitores().get(0).inscrever();

        //Não pode repetir
        peer.addCandidato(cand, null, null);
        peer.addCandidato(cand, null, null);
        for(Candidato c : peer.getCandidatos()){
            if(c.equals(cand)){
                cnt++;
            }
        }
        assertEquals(cnt, 1);
    }

    //Voto
    @Test
    public void testVoto(){
        peer.addEleitor(new Eleitor(321, "teste"));
        peer.getEleitores().get(0).inscrever();
        peer.addCandidato(new Candidato(123, "teste", 321L), null, null);
        peer.getCandidatos().get(0).confirma();

        Mensagem msg = new Mensagem();
        msg.setParam("titulo", "321");
        msg.setParam("nome", "teste");
        msg.setParam("cand", "123");

        //Eleições devem ter iniciado
        peer.realizarVoto(msg, null);
        assertEquals(peer.getVotosQtd(), 0);
        peer.progress();

        //Apenas 1 voto
        peer.realizarVoto(msg, null);
        assertEquals(peer.getVotosQtd(), 1);

        //Eleitor não pode votar mais
        peer.realizarVoto(msg, null);
        assertEquals(peer.getVotosQtd(), 1);
    }
}
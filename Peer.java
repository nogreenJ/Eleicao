/*

Codigo de acordo com o tutorial abaixo. Favor ler para compreender melhor o funcionamento da aplicação.

http://www.jgroups.org/tutorial4/index.html

*/

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import util.*;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

/*Como o SimpleChat é um ReceiverAdapter, ele é capaz de além de enviar mensagens,também receber mensagens de forma assíncrona */
public class Peer extends ReceiverAdapter {

    //Todos possuem
    JChannel channel;
    Eleitor eleitor = new Eleitor();
    View viewAtual;
    EleicaoStatus eleicaoIniciada = EleicaoStatus.PRE;
    String screen = "";
    Integer votou = 0; //utilizado na apuracao de erros pelo coord
    State state = new State();

    //Todos possuem mas é cadastros/pedidos para coordenador
    List<Eleitor> eleitores = new ArrayList<>();

    //apenas coordenador possui
    List<Candidato> candidatos = new ArrayList<>();

    /*Toda a vez que um peer entra ou sai do grupo é enviado um objeto View, que contém informações sobre todos os peers */
    public void viewAccepted(View newView) {
        viewAtual = newView;
    }

    /*Função que recebe uma mensagem e faz o print desta. Método é chamado toda a vez que chega uma mensagem, pelo receiver */
    public void receive(Message msg) {
        //Faz parser do payload de Message para Mensagem
        Mensagem m = (Mensagem) msg.getObject();
        Mensagem resposta = new Mensagem();
        if(m.getStatus() == Status.NOAUTH){
            screen = "\nVOCÊ NÃO ESTÁ AUTENTICADO!";
        } else {
            switch(m.getOperacao()){
        
            //PEERS
            case "VOTOCONF":
                screen = "\nSEU VOTO FOI: ";
                switch(m.getStatus()){
                    case OK:
                        screen += "ACEITO!";
                    break;
                    case ERROR:
                        screen += "NEGADO! Número de candidato inexistente!";
                        votou = 0;
                    break;
                    case PARAMERROR:
                        screen += "NEGADO! Seu título e/ou nome inserido foram incorretos!";
                    break;
                    default:
                        screen += "INCONCLUSIVO?";
                    break;
                }
            break;
            case "CANDCONF":
                screen = "\nSEU PEDIDO DE SER CANDIDATO: ";
                switch(m.getStatus()){
                    case OK:
                        screen += "ESTÁ AGUARDANDO APROVAÇÃO!";
                    break;
                    case ERROR:
                        screen += "FOI NEGADO! Número de candidato já ocupado, você já é candidato ou votação já iniciou";
                        votou = 0;
                    break;
                    case PARAMERROR:
                        screen += "FOI NEGADO! Seu título e/ou nome inserido foram incorretos!";
                    break;
                    default:
                        screen += "FOI INCONCLUSIVO?";
                    break;
                }
            break;
            case "COUNTCONF":
                switch(m.getStatus()){
                    case OK:
                        try {
                            ObjectInputStream ois = new ObjectInputStream(
                                new ByteArrayInputStream(m.getParam("votos").getBytes())
                            );
                            List<Candidato> candRec = (List<Candidato>) ois.readObject();
                            ois.close();

                            resposta = new Mensagem("COUNTRESP");
                            resposta.setStatus(Status.OK);
                            //VER SE TEM MESMO NR DE CANDIDATOS
                            if(candRec.size() == state.getCandidatos().size()){
                                //VAI VERIFICAR SE TEM MESMOS CANDIDATOS COM MESMOS VOTOS
                                for(Candidato c : state.getCandidatos()){
                                    if(!candRec.contains(c)){
                                        resposta.setStatus(Status.ERROR);
                                    }
                                }
                            } else {
                                resposta.setStatus(Status.ERROR);
                            }
                            try {
                                channel.send(msg.getSrc(), resposta);
                            } catch (Exception e) {
                                //TODO: handle exception
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    break;
                    case ERROR:
                        screen = "\nVOCÊ DEVE ESPERAR A ELEIÇÃO ACABAR PARA PEDIR UMA DEPURAÇÃO";
                    break;
                    default:
                    break;
                }
            break;
            case "COUNTRESPCONF":
                screen = "\n" + m.getParam("msg");
            break;
            case "AUTHRESP":
                switch(m.getStatus()){
                    case OK:
                        screen += "\nACESSO LIBERADO!";
                        eleitor = new Eleitor(Long.parseLong(m.getParam("titulo")), m.getParam("nome"));
                        eleitor.inscrever(Long.parseLong(m.getParam("titulo")));
                    break;
                    case ERROR:
                        screen += "\nELEITOR JÁ LOGADO!";
                    break;
                    case PARAMERROR:
                        screen += "\nDADOS INCORRETOS!";
                    break;
                    default:
                        screen += "VOTOS ATUALIZADOS? TALVEZ?";
                    break;
                }
            break;
            case "CANDRESP":
                screen = "\nSEU PEDIDO DE SER CANDIDATO FOI: ";
                switch(m.getStatus()){
                    case OK:
                        screen += "ACEITO!";
                    break;
                    case ERROR:
                        screen += "NEGADO!";
                        votou = 0;
                    break;
                    default:
                        screen += "INCONCLUSIVO?";
                    break;
                }
            break;
            case "COUNTRESP":
                switch(m.getStatus()){
                    case ERROR:
                        votou++;
                    break;
                    default:
                    break;
                }

                resposta = new Mensagem("COUNTRESPCONF");
                if(votou > 0){
                    resposta.setParam("msg", votou + " ERRO(S) DE CONTAGEM!");
                    resposta.setStatus(Status.ERROR);
                } else {
                    resposta.setParam("msg", "SEM ERROS DE CONTAGEM!");
                    resposta.setStatus(Status.OK);
                }

                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            break;
            case "START": case "END":
                progress();
            break;

            //COORDENADOR
            case "VOTO":
                resposta = new Mensagem("VOTOCONF");
                resposta.setStatus(Status.OK);
                if(!auth(Long.parseLong(m.getParam("titulo")), m.getParam("nome"))){
                    resposta.setStatus(Status.PARAMERROR);
                } else if (!candidatoExists(Integer.parseInt(m.getParam("cand")))){
                    resposta.setStatus(Status.ERROR);
                }
                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            break;
            case "CAND":
                resposta = new Mensagem("CANDCONF");
                resposta.setStatus(Status.OK);
                Candidato c = new Candidato(Integer.parseInt(m.getParam("nome")), m.getParam("nome"));
                c.setEndereco(msg.getSrc());
                if(!auth(Long.parseLong(m.getParam("titulo")), m.getParam("nome"))){
                    resposta.setStatus(Status.PARAMERROR);
                    break;
                } else if (eleicaoIniciada.equals(EleicaoStatus.DUR) || candidatos.contains(c)){
                    resposta.setStatus(Status.ERROR);
                    break;
                } else {
                    candidatos.add(c);
                }
                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    //TODO: handle exception
                    e.printStackTrace();
                }
            break;
            case "AUTH":
                resposta = new Mensagem("AUTHRESP");
                resposta.setStatus(Status.OK);
                if(eleitorInscrito(Long.parseLong(m.getParam("titulo")), m.getParam("nome"))){
                    resposta.setStatus(Status.ERROR);
                } else if (!auth(Long.parseLong(m.getParam("titulo")), m.getParam("nome"))){
                    resposta.setStatus(Status.PARAMERROR);
                } else {
                    for(int i = 0; i < eleitores.size(); i++){
                        if(eleitores.get(i).getTitulo() == Long.parseLong(m.getParam("titulo")) &&
                            eleitores.get(i).getNome().equals(m.getParam("nome"))){
                                
                            eleitores.get(i).inscrever(Long.parseLong(m.getParam("titulo")));
                            eleitores.get(i).setEndereco(msg.getSrc());
                            resposta.setParam("titulo", m.getParam("titulo"));
                            resposta.setParam("nome", m.getParam("nome"));
                            break;
                        }
                    }
                }

                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    //TODO: handle exception
                    e.printStackTrace();
                }
            break;
            case "COUNT":
                resposta = new Mensagem("COUNTCONF");
                if(!eleicaoIniciada.equals(EleicaoStatus.POS)){
                    resposta.setStatus(Status.ERROR);
                    try {
                        channel.send(msg.getSrc(), resposta);
                    } catch (Exception e) {
                        //TODO: handle exception
                        e.printStackTrace();
                    }
                } else {
                    try {
                        resposta.setStatus(Status.OK);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(candidatos);
                        resposta.setParam("votos", bos.toByteArray().toString());
                        channel.send(null, resposta);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            break;
            default:
                screen = "\nChegou mensagem sem status padrão definido: " + m.toString();
        }
        }

        //aqui é feita a atualização do state, que é compartilhado com todos os peers
        synchronized(/*votos e autenticacao */state) {
            //state.add(usua); //é atualizado com a string da mensagem
        }
    }

    /*recebe o state de outro processo */
    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    /*faz a atualização do state local */
    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        List<Candidato> listVotos = (List<Candidato>) Util.objectFromStream(new DataInputStream(input));
        synchronized(state) {
            state = new State();
            state.setCandidatos(listVotos);
        }
    }

    /*Cria o canal de Comunicação se não existir e passa a ser o coordenador, senão entra no canal */
    private void start() throws Exception {
        //criação do canal e configuração do receiver
        channel=new JChannel().setReceiver(this);
        channel.connect("ChatCluster");
        //faz o pedido do state global ao processo coordenador
        channel.getState(null, 10000);
        //abre event loop para envio de mensagens
        eventLoop();
        channel.close();
    }

    /*Event */
    private void eventLoop() {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                mostraMenu(screen);
                System.out.print("> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                Mensagem m = new Mensagem();
                Message msg = new Message(viewAtual.getCoord(), m);
                screen = "";
                if(!eleitor.isInscrito() || line.equals("4") || isCoordenador()){
                    screen = "\nIDENTIFIQUE-SE PRIMEIRO!!";
                }else{
                    switch(line){
                    case "help":
                        screen += 
                            "\n1 - Votar" +
                            "\n2 - Mostrar usuários" +
                            "\n3 - Mostrar candidatos" +
                            "\n4 - Identificar-se" +
                            "\n5 - Candidatar-se"+//;
                        //if(isCoordenador()){
                            //screen += 
                            "\n6 - Cadastrar usuários" +
                            "\n7 - Confirmar candidatos" +
                            "\n8 - Iniciar eleicao" +
                            "\n9 - Terminar eleicao"; 
                        //}*/
                    break;
                    case "1":
                        if(!eleicaoIniciada.equals(EleicaoStatus.DUR) || votou != 0){
                            screen += "\nVOCÊ JÁ VOTOU! NO CANDIDATO NR " + votou;
                            break;
                        }
                        m = new Mensagem("VOTO");

                        System.err.print("INSIRA O CANDIDATO PARA QUEM QUER VOTAR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("cand", line);
                        
                        System.err.print("INSIRA O SEU NOME: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("nome", line);
                        
                        System.err.print("INSIRA O SEU TITULO DE ELEITOR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("titulo", line);

                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg);
                    break;
                    case "2":
                        screen = mostraUsuarios();
                    break;
                    case "3":
                        m = new Mensagem("SHOWCAND");
                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg);
                    break;
                    case "4":
                        if(eleitor.isInscrito()){
                            break;
                        }
                        m = new Mensagem("");

                        System.err.print("INSIRA O SEU NOME: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("nome", line);

                        System.err.print("INSIRA O SEU TÍTULO DE ELEITOR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("titulo", line);

                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg);
                    break;
                    case "5":
                        if(isCoordenador() || !eleicaoIniciada.equals(EleicaoStatus.PRE)){
                            break;
                        }
                        m = new Mensagem("CAND");

                        System.err.print("INSIRA O SEU TÍTULO DE ELEITOR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("titulo", line);

                        System.err.print("INSIRA O SEU NÚMERO DE CANDIDATO: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("nrcand", line);

                        m.setParam("nome", eleitor.getNome());
                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg); 
                    break;
                    case "6":
                        if(!isCoordenador()){
                            break;
                        }
                        Eleitor newEleitor = new Eleitor();
                        while(true){
                            System.err.print("INSIRA O NOME DO ELEITOR (0 para sair): ");
                            System.err.print("> "); System.out.flush();
                            line = in.readLine().toLowerCase();
                            if(line.equals("0")){break;}
                            newEleitor.setNome(line);
                            
                            System.err.print("INSIRA O SEU TÍTULO DE ELEITOR (0 para sair): ");
                            System.err.print("> "); System.out.flush();
                            line = in.readLine().toLowerCase();
                            if(line.equals("0")){break;}
                            newEleitor.setTitulo(Long.parseLong(line));

                            eleitores.add(newEleitor);
                        }
                    break;
                    case "7":
                        if(!isCoordenador()){
                            break;
                        }
                        while(true){
                            Runtime.getRuntime().exec("cls");
                            screen = printCandidatos();

                            System.err.print("INSIRA O CANDIDATO A EDITAR (0 para retornar): ");
                            System.err.print("> "); System.out.flush();
                            line = in.readLine().toLowerCase();
                            if(line.equals("0")){break;}
                            
                            Integer index = 0, count = 0;
                            for(Candidato c : state.getCandidatos()){
                                count ++;
                                if(c.getNumero().toString().equals(line)){
                                    index = count;
                                    break;
                                }
                            }
                            if(index != 0){
                                System.err.print("INSIRA O QUE DESEJA FAZER (0 para retornar): ");
                                System.err.print("1 - Confirmar 2 - Negar ");
                                System.err.print("> "); System.out.flush();
                                line = in.readLine().toLowerCase();
                                if(line.equals("0")){break;}

                                Mensagem resposta = new Mensagem("CANDRESP");
                                if(line.equals("1")){
                                    state.getCandidatos().get(index).confirma();
                                    resposta.setStatus(Status.OK);
                                } else if(line.equals("2")) {
                                    state.getCandidatos().remove(index);
                                    resposta.setStatus(Status.ERROR);
                                }
                                try {
                                    channel.send(state.getCandidatos().get(index).getEndereco(), resposta);
                                } catch (Exception e) {
                                    //TODO: handle exception
                                    e.printStackTrace();
                                }
                            }
                        }
                    break;
                    case "8":
                        if(!isCoordenador()){
                            break;
                        }
                        m = new Mensagem("START");
                        msg = new Message(null, m);
                        channel.send(msg); 
                        eleicaoIniciada = EleicaoStatus.DUR;
                    break;
                    case "9":
                        if(!isCoordenador()){
                            break;
                        }
                        m = new Mensagem("END");
                        msg = new Message(null, m);
                        channel.send(msg); 
                        eleicaoIniciada = EleicaoStatus.POS;
                    break;
                    default:
                        System.err.println("Comando errado, pressione \"help\" para ver comandos.");
                }
                }
                Runtime.getRuntime().exec("cls");
                if(line.startsWith("quit") || line.startsWith("exit")) {
                    screen = "\nAté mais!";
                    break;
                }
                System.err.println(screen + printVotos());
            }
            catch(Exception e) {
            }
        }
    }

    private String printCandidatos(){
        String ret = "\tNOME\tNUMERO\tCONF\n";
        for(Candidato c : state.getCandidatos()){
            ret += c.toString() + "\n";
        }
        return ret;
    }

    private String printVotos(){
        if(eleicaoIniciada.equals(EleicaoStatus.PRE)){
            return "";
        }
        String ret = "\n\n\tNOME\tNUMERO\tCONF\tVOTOS\n";
        for(Candidato c : state.getCandidatos()){
            ret += c.toString() + "\n";
        }
        return ret;
    }

    public void progress(){
        if(eleicaoIniciada.equals(EleicaoStatus.PRE)){
            eleicaoIniciada = EleicaoStatus.DUR;
        } else {
            eleicaoIniciada = EleicaoStatus.POS;
        }
    }

    private boolean eleitorInscrito(Long nr, String nome){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr && e.getNome().equals(nome)){
                return e.isInscrito();
            }
        }
        return false;
    }

    private boolean auth(Long nr, String nome){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr && e.getNome().equals(nome)){
                return true;
            }
        }
        return false;
    }

    private boolean candidatoExists(Integer nr){
        for(int i = 0; i < candidatos.size(); i++){
            if(candidatos.get(i).getNumero() == nr){
                candidatos.get(i).vota();
                state.getCandidatos().get(i).vota();
                return true;
            }
        }
        return false;
    }

    private String mostraUsuarios(){
        String users = "";
        return users;
    }

    private void mostraMenu(String screen){
        System.err.println(screen);
    }

    public static void main(String[] args) throws Exception {
        new Peer().start();
    }

    private boolean isCoordenador(){
        return channel.getAddress().equals(viewAtual.getCoord());
    }
}
/*

Codigo de acordo com o tutorial abaixo. Favor ler para compreender melhor o funcionamento da aplicacao.

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

/*Como o SimpleChat e um ReceiverAdapter, ele e capaz de alem de enviar mensagens,tambem receber mensagens de forma assincrona */
public class Peer extends ReceiverAdapter {

    //Todos possuem
    private JChannel channel;
    private View viewAtual;
    private Eleitor eleitor = new Eleitor();
    private EleicaoStatus eleicaoIniciada = EleicaoStatus.PRE;
    private String screen = "";
    private Integer votou = 0; //utilizado na apuracao de erros pelo coord
    private State state = new State();

    //Utilizado para confiracao pos eleicao
    private Map<Integer, Integer> apur = new HashMap<>();

    //apenas coordenador possui
    private Integer votosQtd = 0;
    private List<Eleitor> eleitores = new ArrayList<>();
    private List<Candidato> candidatos = new ArrayList<>();

    public void setEleitores(List<Eleitor> eleitores){
        this.eleitores = eleitores;
    }

    public List<Eleitor> getEleitores(){
        return eleitores;
    }

    /*Toda a vez que um peer entra ou sai do grupo e enviado um objeto View, que contem informacões sobre todos os peers */
    public void viewAccepted(View newView) {
        viewAtual = newView;
    }

    /*Funcao que recebe uma mensagem e faz o print desta. Metodo e chamado toda a vez que chega uma mensagem, pelo receiver */
    public void receive(Message msg) {
        //Faz parser do payload de Message para Mensagem
        Mensagem m = (Mensagem) msg.getObject();
        Mensagem resposta = new Mensagem();
        
        switch(m.getOperacao()){
            //PEERS
            case "VOTOCONF":
                screen = "\nSEU VOTO FOI: ";
                switch(m.getStatus()){
                    case OK:
                        screen += "ACEITO!";
                    break;
                    case ERROR:
                        screen += "NEGADO! Numero de candidato inexistente!";
                        votou = 0;
                    break;
                    case PARAMERROR:
                        screen += "NEGADO! Seu titulo e/ou nome inserido foram incorretos, ou voce ja votou!";
                        votou = 0;
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
                        screen += "ESTA AGUARDANDO APROVACAO!";
                    break;
                    case ERROR:
                        screen += "FOI NEGADO! Numero de candidato ja ocupado, voce ja e candidato ou votacao ja iniciou";
                        votou = 0;
                    break;
                    case PARAMERROR:
                        screen += "FOI NEGADO! Seu titulo e/ou nome inserido foram incorretos!";
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
                        screen = "\nVOCE DEVE ESPERAR A ELEICAO ACABAR PARA PEDIR UMA DEPURACAO";
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
                        screen += "\nACESSO LIBERADO! BEM VINDO(A) " + m.getParam("nome");
                        eleitor = new Eleitor(Long.parseLong(m.getParam("titulo")), m.getParam("nome"));
                        eleitor.inscrever();
                    break;
                    case ERROR:
                        screen += "\nELEITOR JA LOGADO!";
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
            case "START": 
                progress();
            break;
            case "END":
                progress();
                state.loadCandidatosCom(m.getParam("votos"));
                screen = "\nELEICAO TERMINADA!\n" + printVotos();
            break;
            case "SHOWCANDRESP":
                screen += m.getParam("votos");
            break;
            case "APUR":   
                resposta = new Mensagem("APURRESP");
                resposta.setParam("voto", "" + votou);
                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            break;
            case "APURRESP":
                addApur(m.getParam("voto"));  
                resposta = new Mensagem("APURCONF");
                resposta.setParam("votos", apurToString());
                try {
                    channel.send(viewAtual.getCoord(), resposta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            break;
            case "ERR":
                eleicaoIniciada = EleicaoStatus.ERR;
            break;
            case "SHOWUSERSRESP":
                screen += m.getParam("users");
            break;

            //COORDENADOR
            case "SHOWUSERS":
                resposta = new Mensagem("SHOWUSERSRESP");
                resposta.setStatus(Status.OK);
                resposta.setParam("users", mostraUsuarios());
                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            break;
            case "APURCONF":
                apurFromString(m.getParam("votos"));
                if(!confirmApur()){
                    resposta = new Mensagem("ERR");
                    eleicaoIniciada = EleicaoStatus.ERR;
                    try {
                        channel.send(null, resposta);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            break;
            case "SHOWCAND":
                resposta = new Mensagem("SHOWCANDRESP");
                resposta.setStatus(Status.OK);
                resposta.setParam("votos", printCandidatos());
                try {
                    channel.send(msg.getSrc(), resposta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            break;
            case "VOTO":
                resposta = new Mensagem("VOTOCONF");
                resposta.setStatus(Status.OK);
                if(!eleitorInscrito(Long.parseLong(m.getParam("titulo")), m.getParam("nome")) ||//Eleitor n se inscrevei
                    eleitorVotou(Long.parseLong(m.getParam("titulo"))) || //Eleitor ja votou
                    !authAddr(msg.getSrc(), Long.parseLong(m.getParam("titulo")))/*Endereco invalido */){
                    resposta.setStatus(Status.PARAMERROR);
                } else if (!candidatoExists(Integer.parseInt(m.getParam("cand")))){
                    resposta.setStatus(Status.ERROR);
                } else {
                    for(int i = 0; i < eleitores.size(); i++){
                        if(eleitores.get(i).getTitulo() == Long.parseLong(m.getParam("titulo"))){
                            eleitores.get(i).vota();
                            votosQtd++;
                        }
                    }
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
                Candidato c = new Candidato();
                c.setNumero(Integer.parseInt(m.getParam("nrcand")));
                c.setNome(m.getParam("nome"));
                c.setTitulo(Long.parseLong(m.getParam("titulo")));
                c.setEndereco(msg.getSrc());
                
                if(!eleitorInscrito(c.getTitulo(), c.getNome()) || !authAddr(msg.getSrc(), Long.parseLong(m.getParam("titulo")))){
                    resposta.setStatus(Status.PARAMERROR);
                } else if (eleicaoIniciada.equals(EleicaoStatus.DUR) || numeroCandRepetido(c.getNumero())){
                    resposta.setStatus(Status.ERROR);
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
                if(eleitorInscrito(Long.parseLong(m.getParam("titulo")))){
                    resposta.setStatus(Status.ERROR);
                } else if (!auth(Long.parseLong(m.getParam("titulo")))){
                    resposta.setStatus(Status.PARAMERROR);
                } else {
                    for(int i = 0; i < eleitores.size(); i++){
                        if(eleitores.get(i).getTitulo() == Long.parseLong(m.getParam("titulo"))){
                            eleitores.get(i).inscrever();
                            eleitores.get(i).setEndereco(msg.getSrc());
                            if(eleicaoIniciada == EleicaoStatus.DUR || eleicaoIniciada == EleicaoStatus.POS){
                                resposta = new Mensagem("START");
                                try {
                                    channel.send(msg.getSrc(), resposta);
                                } catch (Exception e) {
                                    //TODO: handle exception
                                    e.printStackTrace();
                                }
                            }
                            if(eleicaoIniciada == EleicaoStatus.POS){
                                resposta = new Mensagem("END");
                                try {
                                    channel.send(msg.getSrc(), resposta);
                                } catch (Exception e) {
                                    //TODO: handle exception
                                    e.printStackTrace();
                                }
                            }
                            if(eleicaoIniciada == EleicaoStatus.ERR){
                                resposta = new Mensagem("ERR");
                                try {
                                    channel.send(msg.getSrc(), resposta);
                                } catch (Exception e) {
                                    //TODO: handle exception
                                    e.printStackTrace();
                                }
                            }
                            resposta = new Mensagem("AUTHRESP");
                            resposta.setStatus(Status.OK);
                            resposta.setParam("titulo", "" + eleitores.get(i).getTitulo());
                            resposta.setParam("nome", eleitores.get(i).getNome());
                            state.add(eleitores.get(i));
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
                screen = "\nChegou mensagem sem status padrao definido: " + m.toString();
        }
        System.err.println("\n\n\n\n\n" + isErr() + screen + "\n> ");

        synchronized(state) {
            
        }
    }

    private String isErr(){
        if(eleicaoIniciada == EleicaoStatus.ERR){
            return "\n\n\nERRO NA CONTAGEM DOS VOTOS!!!!!!!";
        }
        return "";
    }

    /*recebe o state de outro processo */
    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    /*faz a atualizacao do state local */
    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        State stateNew = (State) Util.objectFromStream(new DataInputStream(input));
        synchronized(state) {
            state = stateNew;
        }
    }

    /*Cria o canal de Comunicacao se nao existir e passa a ser o coordenador, senao entra no canal */
    private void start() throws Exception {
        //criacao do canal e configuracao do receiver
        channel=new JChannel().setReceiver(this);
        channel.connect("ChatCluster");
        channel.setDiscardOwnMessages(true);
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
                System.out.print("> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                Mensagem m = new Mensagem();
                Message msg = new Message(viewAtual.getCoord(), m);
                screen = "";
                switch(line){
                    case "help":
                        screen += 
                            "\n1 - Votar" +
                            "\n2 - Mostrar usuarios" +
                            "\n3 - Mostrar candidatos/apurar votos se depois da eleicao" +
                            "\n4 - Identificar-se" +
                            "\n5 - Candidatar-se" + 
                            "\n6 - Cadastrar usuarios" +
                            "\n7 - Confirmar candidatos" +
                            "\n8 - Iniciar eleicao" +
                            "\n9 - Terminar eleicao";  
                    break;
                    case "1":
                        if(isCoordenador()){
                            screen = "O COORDENADOR NAO PODE VOTAR";
                            break;
                        }
                        if(!eleicaoIniciada.equals(EleicaoStatus.DUR)){
                            screen = "SO E POSSIVEL VOTAR DURANTE AS ELEICOES";
                            break;
                        }
                        screen = "";
                        m = new Mensagem("VOTO");
                        
                        System.err.print("INSIRA O SEU NOME: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("nome", line);
                        
                        System.err.print("INSIRA O SEU TITULO DE ELEITOR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("titulo", line);
                        
                        System.err.print("INSIRA O CANDIDATO QUE IRÁ VOTAR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("cand", line);
                        votou = Integer.parseInt(line);

                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg);
                        screen = "";
                    break;
                    case "2":
                        screen = mostraUsuarios();
                        if(isCoordenador()){
                            screen = mostraUsuarios();
                        } else {
                            screen = "";
                            m = new Mensagem("SHOWUSERS");
                            msg = new Message(viewAtual.getCoord(), m);
                            channel.send(msg); 
                        }
                    break;
                    case "3":
                        if(isCoordenador()){
                            screen = printCandidatos();
                        } else {
                            if(eleicaoIniciada.equals(EleicaoStatus.POS)){
                                screen = "";
                                apur = new HashMap<>();
                                if(votou != 0){
                                    apur.put(votou, 1);
                                }
                                m = new Mensagem("APUR");
                                msg = new Message(null, m);
                                channel.send(msg); 
                            }
                            screen = "";
                            m = new Mensagem("SHOWCAND");
                            msg = new Message(viewAtual.getCoord(), m);
                            channel.send(msg); 
                        }
                    break;
                    case "4":
                        if(isCoordenador()){
                            screen = "COORDENADOR NAO PRECISA SE AUTENTICAR";
                            break;
                        }
                        if(eleitor.isInscrito()){
                            screen = "VOCE JA ESTA AUTENTICADO";
                            break;
                        }
                        screen = "";
                        m = new Mensagem("AUTH");

                        System.err.print("INSIRA O SEU TITULO DE ELEITOR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("titulo", line);

                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg);
                        screen = "";
                    break;
                    case "5":
                        if(!eleicaoIniciada.equals(EleicaoStatus.PRE)){
                            screen = "SO E POSSIVEL SE CANDIDATAR ANTES DAS ELEICOES";
                            break;
                        }
                        if(isCoordenador()){
                            screen = "O COORDENADOR NAO PODE SE CANDIDATAR";
                            break;
                        }
                        screen = "";
                        m = new Mensagem("CAND");
                        
                        System.err.print("INSIRA O SEU NOME: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("nome", line);

                        System.err.print("INSIRA O SEU TITULO DE ELEITOR: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("titulo", line);

                        System.err.print("INSIRA O SEU NUMERO DE CANDIDATO: ");
                        System.err.print("> "); System.out.flush();
                        line = in.readLine().toLowerCase();
                        m.setParam("nrcand", line);

                        msg = new Message(viewAtual.getCoord(), m);
                        channel.send(msg); 
                        screen = "";
                    break;
                    case "6":
                        if(!isCoordenador()){
                            break;
                        }
                        screen = "";
                        while(true){
                            Eleitor newEleitor = new Eleitor();
                            System.err.print("INSIRA O NOME DO ELEITOR (0 para sair): ");
                            System.err.print("> "); System.out.flush();
                            line = in.readLine().toLowerCase();
                            if(line.equals("0")){break;}
                            newEleitor.setNome(line);
                            
                            System.err.print("INSIRA O SEU TITULO DE ELEITOR (0 para sair): ");
                            System.err.print("> "); System.out.flush();
                            line = in.readLine().toLowerCase();
                            if(line.equals("0")){break;}
                            newEleitor.setTitulo(Long.parseLong(line));

                            if(!addEleitor(newEleitor)){
                                System.err.println("ELEITOR COM DADOS REPETIDOS NAO CADASTRADO");
                            }
                        }
                    break;
                    case "7":
                        if(!isCoordenador()){
                            break;
                        }
                        screen = "";
                        if(eleicaoIniciada != EleicaoStatus.PRE){
                            screen = "SO E POSSIVEL EDITAR CANDIDATOS ANTES DA ELEICAO";
                            break;
                        }
                        while(true){
                            System.err.println(printCandidatosAgu());

                            System.err.print("INSIRA O CANDIDATO A EDITAR (0 para retornar): ");
                            System.err.print("> "); System.out.flush();
                            line = in.readLine().toLowerCase();
                            if(line.equals("0")){break;}
                            
                            Integer index = -1, count = 0;
                            for(Candidato c : candidatos){
                                if(c.getNumero().toString().equals(line)){
                                    index = count;
                                    break;
                                }
                                count ++;
                            }
                            if(index != -1 && !candidatos.get(index).confirmado()){
                                System.err.print("INSIRA O QUE DESEJA FAZER (0 para retornar): ");
                                System.err.print("\n1 - Confirmar 2 - Negar ");
                                System.err.print("\n> "); System.out.flush();
                                line = in.readLine().toLowerCase();
                                if(line.equals("0")){break;}

                                Mensagem resposta = new Mensagem("CANDRESP");
                                if(line.equals("1")){
                                    candidatos.get(index).confirma();
                                    state.add(candidatos.get(index));
                                    //candidatos.remove(index);
                                    resposta.setStatus(Status.OK);

                                    try {
                                        channel.send(state.getCandidatos().get(index).getEndereco(), resposta);
                                    } catch (Exception e) {
                                        //TODO: handle exception
                                        e.printStackTrace();
                                    }
                                } else if(line.equals("2")) {
                                    //candidatos.remove(index);
                                    resposta.setStatus(Status.ERROR);

                                    try {
                                        channel.send(candidatos.get(index).getEndereco(), resposta);
                                    } catch (Exception e) {
                                        //TODO: handle exception
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        screen = "";
                    break;
                    case "8":
                        if(!isCoordenador()){
                            break;
                        }
                        screen = "";
                        if(state.getCandidatos().size() <= 1){
                            screen = "NAO E POSSIVEL INICIAR A ELEICAO COM UM OU NENHUM CANDIDATO";
                            break;
                        }
                        if(eleicaoIniciada != EleicaoStatus.PRE){
                            screen = "SO E POSSIVEL INICIAR A ELEICAO ANTES DELA";
                            break;
                        }
                        m = new Mensagem("START");
                        msg = new Message(null, m);
                        channel.send(msg); 
                        eleicaoIniciada = EleicaoStatus.DUR;
                        screen = "";
                    break;
                    case "9":
                        if(!isCoordenador()){
                            break;
                        }
                        if(eleicaoIniciada != EleicaoStatus.DUR){
                            screen = "SO E POSSIVEL TERMINAR A ELEICAO DURANTE ELA";
                            break;
                        }
                        screen = "";
                        m = new Mensagem("END");
                        m.setParam("votos", state.getCandidatosString(true));
                        msg = new Message(null, m);
                        channel.send(msg); 
                        eleicaoIniciada = EleicaoStatus.POS;
                        screen = "";
                    break;
                    case "exit": case "quit":
                        System.err.println("\nAte mais!");
                    break;
                    default:
                        System.err.println("Comando errado, pressione \"help\" para ver comandos.");
                }
                if(line.equals("exit") || line.equals("quit")) {
                    break;
                }
                System.err.println("\n\n\n\n\n" + isErr() + screen + "\n> ");
            }
            catch(Exception e) {
            }
        }
    }

    private boolean numeroCandRepetido(Integer n){
        for(Candidato c : candidatos){
            if(c.getNumero() == n){
                return true;
            }
        }
        return false;
    }

    public boolean addEleitor(Eleitor eleitor){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == eleitor.getTitulo() || e.getNome().equals(eleitor.getNome())){
                return false;
            }
        }
        eleitores.add(eleitor);
        return true;
    }

    private String printCandidatosAgu(){
        String ret = "\nNOME\tNUMERO\tCONF\n";

        for(Candidato c : candidatos){
            ret += c.toString() + "\n";
        }
        return ret;
    }

    private String printCandidatos(){
        String ret = "\nNOME\tNUMERO\tCONF\n";

        for(Candidato c : state.getCandidatos()){
            ret += c.toString() + "\n";
        }
        return ret;
    }

    private String printVotos(){
        if(eleicaoIniciada.equals(EleicaoStatus.PRE)){
            return "";
        }
        String ret = "\n\nNOME\tNUMERO\tCONF\tVOTOS\n";
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

    private Eleitor getByNr(Long nr){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr){
                return e;
            }
        }
        return null;
    }

    private boolean eleitorInscrito(Long nr){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr){
                return e.isInscrito();
            }
        }
        return false;
    }

    private boolean eleitorVotou(Long nr){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr){
                return e.votou();
            }
        }
        return false;
    }

    private boolean eleitorInscrito(Long nr, String nome){
        if(nome.isEmpty()){
            return false;
        }
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr && e.getNome().equals(nome)){
                return e.isInscrito();
            }
        }
        return false;
    }

    private boolean auth(Long nr){
        for(Eleitor e : eleitores){
            if(e.getTitulo() == nr){
                return true;
            }
        }
        return false;
    }

    private void addApur(String voto){
        if(voto.equals("0")){
            return;
        }
        Integer v = Integer.parseInt(voto);
        if(apur.containsKey(v)){
            apur.replace(v, apur.get(v) + 1);
        } else {
            apur.put(v, 1);
        }
        System.err.println(printApur());
    }

    private String printApur(){
        String ret = "\nVOTOS CONFIRMADOS: \n";
        for (Integer name: apur.keySet()) {
            String key = name.toString();
            String value = apur.get(name).toString();
            ret += "\n" + key + " " + value;
        }
        return ret;
    }

    private boolean candidatoExists(Integer nr){
        for(int i = 0; i < candidatos.size(); i++){
            if(candidatos.get(i).getNumero() == nr){
                state.getCandidatos().get(i).vota();
                return true;
            }
        }
        return false;
    }

    private String mostraUsuarios(){
        String users = "NOME\tENDERECO";
        for(Eleitor e : state.getEleitores()){
            users += "\n" + e.getNome() + "\t" + e.getEndereco().toString();
        }
        return users + "\n";
    }

    private void apurFromString(String ap){
        String [] apS = ap.split(",");
        apur = new HashMap<>();
        for(int i = 0; i < apS.length; i+=2){
            apur.put(Integer.parseInt(apS[i]), Integer.parseInt(apS[i+1]));
        }
    }

    private String apurToString(){
        String ret = "";
        for (Integer name: apur.keySet()) {
            String key = name.toString();
            String value = apur.get(name).toString();
            ret += key + "," + value + ",";
        }
        return ret;
    }

    private boolean confirmApur(){
        for (Integer name: apur.keySet()) {
            if(sumApu() > votosQtd){
                return false;
            } else if(getVotosFromNum(name) != apur.get(name) && sumApu() == votosQtd){
                return false;
            }
        }
        return true;
    }

    private Integer sumApu(){
        Integer ret = 0;
        for (Integer name: apur.keySet()) {
            ret += apur.get(name);
        }
        return ret;
    }

    private Integer getVotosFromNum(Integer num){
        for(Candidato c : state.getCandidatos()){
            if(c.getNumero() == num){
                return c.getVotos();
            }
        }
        return 0;
    }

    private boolean authAddr(Address src, Long t){
        Eleitor e = getByNr(t);
        return  src.toString().equals(e.getEndereco().toString());
    }

    public static void main(String[] args) throws Exception {
        new Peer().start();
    }

    private boolean isCoordenador(){
        screen = "VOCE PRECISA SER O COORDENADOR PARA ISSO";
        return channel.getAddressAsString().equals(viewAtual.getCoord().toString());
    }
}
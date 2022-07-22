package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class State implements Serializable{
    
    List<Candidato> candidatos = new ArrayList<>();
    List<Eleitor> eleitores = new ArrayList<>();
    
    public State(){

    }  

    public State(List<Candidato> candidatos){
        this.candidatos = candidatos;
    }

    public List<Candidato> getCandidatos(){
        return this.candidatos;
    }

    public String getCandidatosString(boolean votos){
        String ret = "";
        for(Candidato c : candidatos){
            ret += c.getNome() + ",";
            ret += c.getNumero() + ",";
            ret += (votos ? c.getVotos() : "") + ",";
        }
        return "";
    }
    
    public void loadCandidatos(String cand){
        String[] s = cand.split(",");
        candidatos = new ArrayList<>();
        for(int i = 0; i < s.length; i+=2){
            Candidato c = new Candidato();
            c.setNome(s[i]);
            c.setNumero(Integer.parseInt(s[i+1]));
        }
    }
    
    public void loadCandidatosCom(String cand){
        String[] s = cand.split(",");
        candidatos = new ArrayList<>();
        for(int i = 0; i < s.length; i+=3){
            Candidato c = new Candidato();
            c.setNome(s[i]);
            c.setNumero(Integer.parseInt(s[i+1]));
            c.setNumero(Integer.parseInt(s[i+2]));
        }
    }

    public void setCandidatos(List<Candidato> candidatos){
        this.candidatos = candidatos;
    }

    public List<Eleitor> getEleitores(){
        return this.eleitores;
    }

    public void setEleitores(List<Eleitor> eleitores){
        this.eleitores = eleitores;
    }

    public void add(Eleitor eleitor){
        eleitores.add(eleitor);
    }

    public void add(Candidato candidato){
        candidatos.add(candidato);
    }
}

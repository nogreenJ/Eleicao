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

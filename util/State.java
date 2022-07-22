package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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

    public String getCandidatosString(){
        List<Candidato> cand = new ArrayList<>();
        for(Candidato c : candidatos){
            c.setVotos(0);
            cand.add(c);
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream ois = new ObjectOutputStream(baos);
            ois.writeObject(cand);
            byte[] bytes = baos.toByteArray();
            return bytes.toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return "";
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

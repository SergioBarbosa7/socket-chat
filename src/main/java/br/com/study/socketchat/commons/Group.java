package br.com.study.socketchat.commons;

import lombok.Data;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String creator;
    private LocalDateTime createdAt;
    private Set<String> members;

    public Group(String name, String creator) {
        this.name = name;
        this.creator = creator;
        this.createdAt = LocalDateTime.now();
        this.members = ConcurrentHashMap.newKeySet(); // Thread-safe set
        this.members.add(creator);
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override
    public String toString() {
        return String.format("Grupo '%s' (%d membros) - Criado por %s",
                name, members.size(), creator);
    }

    public boolean isGroupEmpty() {
        return members.isEmpty();
    }

    public void addMember(String member) {
        if (members.contains(member)) {
            throw new IllegalArgumentException("Usuário já é membro do grupo");
        }
        members.add(member);
    }

    public void removeMember(String member) {
        if (!members.contains(member)) {
            throw new IllegalArgumentException("Usuário não é um membro");
        }
        members.remove(member);
    }

    public boolean hasMember(String member) {
        return members.contains(member);
    }
}


package br.com.study.socketchat.server.group;

import br.com.study.socketchat.commons.Group;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GroupManager {
    private Map<String, Group> groups;

    public GroupManager() {
        groups = new HashMap<>();
    }

    public void addGroup(Group group) {
        if (groups.containsKey(group.getName())) {
            throw new IllegalArgumentException("Group already exists");
        }
        groups.put(group.getName(), group);
    }

    public List<Group> findAll() {
        return new ArrayList<>(groups.values());
    }

    public void addMemberToGroup(String groupName, String member) {
        Group group = findGroupOrThrow(groupName);
        group.addMember(member);
    }

    public void removeMemberFromGroup(String groupName, String member) {
        Group group = findGroupOrThrow(groupName);
        group.removeMember(member);
        if (group.isGroupEmpty()) {
            System.out.println("Group is empty, so it will be removed");
            groups.remove(groupName);
        }
    }

    public Group findGroupOrThrow(String groupName) {
        if (!groups.containsKey(groupName)) {
            throw new IllegalArgumentException("Group doesn't exist");
        }
        return groups.get(groupName);
    }

    public Group findGroupWithUser(String groupName, String userName) {
        Group group = findGroupOrThrow(groupName);
        if (!group.hasMember(userName)) {
            throw new IllegalArgumentException("Usuário não é um membro");
        }
        return group;
    }
}

package br.com.study.socketchat.server.group.service;

import br.com.study.socketchat.commons.Group;
import br.com.study.socketchat.server.group.GroupManager;

import java.util.List;

public class GroupService {
    private final GroupManager groupManager;

    public GroupService(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    public void createGroup(Group group) {
        groupManager.addGroup(group);
    }

    public void joinGroup(String userName, String groupName) {
        groupManager.addMemberToGroup(groupName, userName);
    }

    public List<Group> findGroups() {
        return groupManager.findAll();
    }

    public void leaveGroup(String username, String groupName) {
        groupManager.removeMemberFromGroup(groupName, username);
    }

    public Group findGroupWithUser(String groupName, String userName) {
        return groupManager.findGroupWithUser(groupName, userName);
    }
}

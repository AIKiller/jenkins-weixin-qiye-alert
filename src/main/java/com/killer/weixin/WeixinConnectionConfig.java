package com.killer.weixin;


import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Descriptor.FormException;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * @author Robin Müller
 */
@Extension
public class WeixinConnectionConfig extends GlobalConfiguration {
	
	
	private transient Map<String, WeixinConnection> connectionMap = new HashMap<>();
	 private List<WeixinConnection> connections = new ArrayList<>();
	
	public WeixinConnectionConfig() {
        load();
 
        refreshConnectionMap();
    }
	
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        connections = req.bindJSONToList(WeixinConnection.class, json.get("connections"));
        refreshConnectionMap();
        save();
        return super.configure(req, json);
    }
    
    private void refreshConnectionMap() {
        connectionMap.clear();
        for (WeixinConnection connection : connections) {
            connectionMap.put(connection.getName(), connection);
        }
        //System.out.println(connectionMap.toString());
    }

    public List<WeixinConnection> getConnections() {
        return connections;
    }

    public void addConnection(WeixinConnection connection) {
        connections.add(connection);
        connectionMap.put(connection.getName(), connection);
    }

    public void setConnections(List<WeixinConnection> newConnections) {
        connections = new ArrayList<>();
        connectionMap = new HashMap<>();
        for (WeixinConnection connection: newConnections){
            addConnection(connection);
        }
    }
    
    
    public ListBoxModel doFillWeixinTokenIdItems(@QueryParameter String name, @QueryParameter String url) {
        if (Jenkins.getInstance().hasPermission(Item.CONFIGURE)) {
        	//System.out.println("doFillWeixinTokenIdItems");
            AbstractIdCredentialsListBoxModel<StandardListBoxModel, StandardCredentials> options = new StandardListBoxModel()
                .includeEmptyValue()
                .includeMatchingAs(ACL.SYSTEM,
                                   Jenkins.getActiveInstance(),
                                   StandardCredentials.class,
                                   URIRequirementBuilder.fromUri(url).build(),
                                   new WeixinCredentialMatcher());
            
            
            if (name != null && connectionMap.containsKey(name)) {
                String apiTokenId = connectionMap.get(name).getWeixinTokenId();
                options.includeCurrentValue(apiTokenId);
                for (ListBoxModel.Option option : options) {
                    if (option.value.equals(apiTokenId)) {
                        option.selected = true;
                    }
                }
            }
            return options;
        }
        return new StandardListBoxModel();
    }
    
    
    private static class WeixinCredentialMatcher implements CredentialsMatcher {
        @Override
        public boolean matches(@NonNull Credentials credentials) {
            try {
                return credentials instanceof WeixinToken || credentials instanceof StringCredentials;
            } catch (Throwable e) {
                return false;
            }
        }
    }
}

package com.killer.weixin;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import static java.util.Collections.sort;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;


/**
 * @author Robin Müller
 */
public class WeixinConnection {
  
	private String name;
	//认证id
	private String weixinTokenId;

	
	private String url;
	//空字符串
	private String isNull = "";
	
	private transient String secret;
    private String corpId;
    private String agentId;
	
    
    
	public String getUrl() {
		return url;
	}

	public String getName() {
		return name;
	}


	public String getWeixinTokenId() {
		return weixinTokenId;
	}

	//用来获取界面上设置的字段信息
    @DataBoundConstructor
    public WeixinConnection(String name,String weixinTokenId,String url) {
        super();
        this.name 			= Util.fixNull(name);
        this.weixinTokenId 	= Util.fixNull(weixinTokenId);
        this.url			= Util.fixNull(url);
        
    }
   
    
    protected WeixinToken getApiToken(String apiTokenId) {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
            lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
            CredentialsMatchers.withId(apiTokenId));
        if (credentials != null) {
            if (credentials instanceof WeixinToken) {
                return ((WeixinToken) credentials);
            }
        }
        throw new IllegalStateException("No credentials found for credentialsId: " + apiTokenId);
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void migrate() throws IOException {
    	WeixinConnectionConfig descriptor = (WeixinConnectionConfig) Jenkins.getInstance().getDescriptor(WeixinConnectionConfig.class);
        for (WeixinConnection connection : descriptor.getConnections()) {
            if (connection.weixinTokenId == null && connection.weixinTokenId != null) {
                for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
                    if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                        List<Domain> domains = credentialsStore.getDomains();
                        connection.weixinTokenId = UUID.randomUUID().toString();
                        credentialsStore.addCredentials(domains.get(0),
                            new WeixinTokenImpl(CredentialsScope.SYSTEM, connection.weixinTokenId, "GitLab API Token", connection.corpId,Secret.fromString(connection.secret),connection.agentId));
                    }
                }
            }
        }
        descriptor.save();
    }
}

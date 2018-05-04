package com.killer.weixin;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;

import hudson.Extension;
import hudson.util.Secret;

import java.util.ArrayList;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Robin Müller
 */
public final class WeixinTokenImpl extends BaseStandardCredentials implements WeixinToken {


	private Secret secret;
    private String corpId;
    private String agentId;

    @DataBoundConstructor
    public WeixinTokenImpl(CredentialsScope scope, String id, String description,String corpId, Secret secret, String agentId) {
        super(scope, id, description);
        this.secret = secret;
        this.corpId = corpId;
        this.agentId = agentId;
    }

    @Override
    public Secret getSecret() {
		return secret;
	}
    @Override
	public String getCorpId() {
		return corpId;
	}
    @Override
	public String getAgentId() {
		return agentId;
	}
    

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return "Weixin API token";
        }
    }
}

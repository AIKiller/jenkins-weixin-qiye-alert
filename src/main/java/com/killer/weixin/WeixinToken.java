package com.killer.weixin;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;

/**
 * @author Robin Müller
 */
@NameWith(WeixinToken.NameProvider.class)
public interface WeixinToken extends StandardCredentials {
	
	Secret getSecret();
	String getCorpId();
	String getAgentId();

    class NameProvider extends CredentialsNameProvider<WeixinToken> {
        @Override
        public String getName(WeixinToken c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return "Weixin API token" + (description != null ? " (" + description + ")" : "");
        }
    }
}

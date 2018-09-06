package com.killer.weixin;

import java.io.IOException;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Cause.UserIdCause;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;

public class WeixinNotifier extends Notifier implements SimpleBuildStep {
	
	/**
	 * 用户患者的微信链接名称
	 */
	private String weixinConnection;
	//微信企业号部门id
	private String partyId;
	//微信企业号用户id
	private String userId;
	//消息推送的标签名称
	private String tagName;
	//分支名称
	private String stage_name;
	
	public String getWeixinConnection() {
		return weixinConnection;
	}
	

	public String getPartyId() {
		return partyId;
	}


	public String getUserId() {
		return userId;
	}


	public String getTagName() {
		return tagName;
	}

	public String getStage_name(){
		return stage_name;
	}

	//用来获取界面上设置的字段信息
    @DataBoundConstructor
    public WeixinNotifier(String weixinConnection,String partyId, String userId,String tagName) {
        super();
        this.partyId 		= Util.fixNull(partyId);
        this.userId 		= Util.fixNull(userId);
        this.tagName 		= Util.fixNull(tagName);
        this.weixinConnection = Util.fixNull(weixinConnection);
    }
    

	public WeixinService newWeixinService(Run<?, ?> run,TaskListener listener) throws IOException, InterruptedException {

		this.stage_name = run.getEnvironment(listener).get("gitlabBranch");
		
		//获取用户设置的所有微信连接实例
		WeixinConnectionConfig descriptor = (WeixinConnectionConfig) Jenkins.getInstance().getDescriptor(WeixinConnectionConfig.class);
		for (WeixinConnection connection : descriptor.getConnections()) {
			//获取用户选择的实例
			//System.out.println(connection.getName());
			//System.out.println("this.weixinConnection:"+this.weixinConnection);
			if(connection.getName().equals(this.weixinConnection)){
				
				return new WeixinServiceImpl(this,connection,run, listener);
			}
        }
		throw new IllegalStateException("No connection found for Weixin: " + this.weixinConnection);
       
    }
	//public String getStage_name(){
	//	return this.run
	//}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Result result = run.getResult();
		// https://github.com/jenkinsci/workflow-basic-steps-plugin/blob/master/CORE-STEPS.md
		//Running a notifier is trickier since normally a pipeline in progress has no status yet
		if(null != result && result.equals(Result.FAILURE)){
        	listener.getLogger().println("项目构建失败,推送通知到微信");
        	this.newWeixinService(run,listener).failure();
        }else if(null != result && result.equals(Result.ABORTED)){
        	listener.getLogger().println("项目构建被终止,推送通知到微信");
        	this.newWeixinService(run,listener).aborted();
        }else if(null != result && result.equals(Result.UNSTABLE)){
        	listener.getLogger().println("项目状态不稳定,推送通知到微信");
        	this.newWeixinService(run,listener).unstable();
        }else{
        	//项目未出现任何异常报错
        	listener.getLogger().println("项目构建结束");
        	this.newWeixinService(run,listener).success();
        }
		//listener.getLogger().println();
		//listener.getLogger().println(run.getEnvironment(listener).toString());
	}
	
	@Override
    public WeixinNotifierDescriptor getDescriptor() {
        return (WeixinNotifierDescriptor) super.getDescriptor();
    }

    @Extension
    @Symbol("weixin")
    public static class WeixinNotifierDescriptor extends BuildStepDescriptor<Publisher> {


        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Weixin message Pusher";
        }

        public ListBoxModel doFillWeixinConnectionItems(){
        	ListBoxModel options = new ListBoxModel();
        	WeixinConnectionConfig descriptor = (WeixinConnectionConfig) Jenkins.getInstance().getDescriptor(WeixinConnectionConfig.class);
            for (WeixinConnection connection : descriptor.getConnections()) {
                options.add(connection.getName(), connection.getName());
            }
            return options;
        }
    }

}

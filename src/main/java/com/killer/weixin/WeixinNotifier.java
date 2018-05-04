package com.killer.weixin;

import java.io.IOException;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;




import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
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

	
	public String getWeixinConnection() {
		return weixinConnection;
	}


	//用来获取界面上设置的字段信息
    @DataBoundConstructor
    public WeixinNotifier(String weixinConnection) {
        super();
        this.weixinConnection = Util.fixNull(weixinConnection);
    }
    


	public WeixinService newWeixinService(Run<?, ?> run,TaskListener listener) {
		//获取用户设置的所有微信连接实例
		WeixinConnectionConfig descriptor = (WeixinConnectionConfig) Jenkins.getInstance().getDescriptor(WeixinConnectionConfig.class);
		for (WeixinConnection connection : descriptor.getConnections()) {
			//获取用户选择的实例
			System.out.println(connection.getName());
			System.out.println("this.weixinConnection:"+this.weixinConnection);
			if(connection.getName().equals(this.weixinConnection)){
				
				return new WeixinServiceImpl(connection,run, listener);
			}
        }
		throw new IllegalStateException("No connection found for Weixin: " + this.weixinConnection);
       
    }


	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
	
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		Result result = run.getResult();
		listener.getLogger().println(run.getFullDisplayName());
        if (null != result && result.equals(Result.SUCCESS)) {
        	listener.getLogger().println("项目构建成功");
        	this.newWeixinService(run,listener).success();
        }else if(null != result && result.equals(Result.FAILURE)){
        	listener.getLogger().println("项目构建失败,推送通知到微信");
        	this.newWeixinService(run,listener).failure();
        }else if(null != result && result.equals(Result.ABORTED)){
        	listener.getLogger().println("项目构建被终止,推送通知到微信");
        	this.newWeixinService(run,listener).aborted();
        }else{
        	listener.getLogger().println("项目状态未知");
        }
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

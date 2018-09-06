package com.killer.weixin;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.Cause.UserIdCause;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpInMemoryConfigStorage;
import me.chanjar.weixin.cp.api.WxCpServiceImpl;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.bean.WxCpMessage.WxArticle;
import me.chanjar.weixin.cp.bean.WxCpTag;
import me.chanjar.weixin.cp.bean.messagebuilder.NewsBuilder;


/**
 * The type Weixin service.
 */
public class WeixinServiceImpl implements WeixinService {


	/**
	 * The Logger.
	 */
	static Logger logger = LoggerFactory.getLogger(WeixinServiceImpl.class);

	/**
	 * 微信配置类
	 */
	static WxCpInMemoryConfigStorage config = new WxCpInMemoryConfigStorage();
	/**
	 * 微信企业号API类
	 */
	static WxCpServiceImpl wxCpService = new WxCpServiceImpl();
	//空字符串
    private String isNull = "";
    
    /**
	 * 微信企业号id
	 */
	private String corpId;
	/**
	 * 微信企业号密钥
	 */

	private String secret;
	/**
	 * 微信企业号应用id
	 */
	private String agentId;
	/**
	 * 微信企业号部门id
	 */
	private String partyId;
	/**
	 * 微信企业号用户id
	 */
	private String userId;
	/**
	 * 微信信息通知标签
	 */
	private String tagName;

	/**
	 * 设置jenkins 消息推送的标题
	 */
	private String title = "Jenkins 消息通知：";
	
	/**
	 * 当前步骤名称，特制，用于丰富报警信息。 
	 */
	private String stage_name;

	/**
	 * The D.
	 */
	Date d = new Date();
	/**
	 * The Sdf.
	 */
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * 设置项目推送的description
	 */
	private String success_context = "%s项目构建成功！分支名称:%s 结束时间：%s 构建耗时:%f秒 操作人员：%s";
	private String failure_context = "%s项目构建失败，详细信息请查看Jenkins构建日志！分支名称:%s 错误步骤名称:%s 结束时间：%s 构建耗时:%f秒 操作人员：%s";
	private String aborted_context = "%s项目被终止构建，请确认该操作是否合法！分支名称:%s 结束时间：%s 构建耗时:%f秒 操作人员：%s";
	private String unstable_context  = "%s项目构建已完成，但项目构建过程中出现警告通知，请及时检查日志并确定项目部署结果是否健康！分支名称:%s 结束时间：%s 构建耗时:%f秒 操作人员：%s";
	
	private WxArticle article = new WxArticle();
	/**
	 * 默认的回调地址
	 */
	private String url = "http://localhost:8080/blue/organizations/jenkins/%s/detail/%s/pipeline";
	

	private Run<?, ?> run;
	/**
	 * 监听器
	 */
	private TaskListener listener;


	/**
	 * Instantiates a new Weixin service.
	 *
	 * @param weixinNotifier the weixin notifier
	 * @param connection     the connection
	 * @param run            the run
	 * @param listener       the listener
	 */
	public WeixinServiceImpl(WeixinNotifier weixinNotifier,WeixinConnection connection,Run<?, ?> run, TaskListener listener) {
		this.partyId 	= weixinNotifier.getPartyId();
		this.userId		= weixinNotifier.getUserId();
		this.tagName	= weixinNotifier.getTagName();
		this.stage_name = weixinNotifier.getStage_name();
		//根据TokenId获取token信息
		WeixinToken weixinToken = connection.getApiToken(connection.getWeixinTokenId());
		this.corpId 	= weixinToken.getCorpId();
		this.secret 	= weixinToken.getSecret().getPlainText();
		this.agentId 	= weixinToken.getAgentId();
		//判断用户是否设置URL参数
		if(!isNull.equals(connection.getUrl())){
			//如果Url为不为空，则使用用户设置的地址
			this.url = connection.getUrl()+"/blue/organizations/jenkins/%s/detail/%s/pipeline";
		}
        this.listener = listener;
        this.run = run;
       
    }


	@Override
    public void success() {
    	//项目构建成功，开始推送信息到微信
		init(success_context,0);//初始化参数
		msg(tagName);//推送信息到微信
    }
    
   

	@Override
	public void failure() {
		init(failure_context,1);//初始化参数
		msg(tagName);//推送信息到微信
	}


	@Override
	public void aborted() {
		init(aborted_context,0);//初始化参数
		msg(tagName);//推送信息到微信
		
	}
	
	@Override
	public void unstable() {
		init(unstable_context,0);//初始化参数
		msg(tagName);//推送信息到微信
	}

	/**
	 * Init.
	 *
	 * @param text       the text
	 * @param is_failure the is failure
	 */
	public void init(String text,int is_failure){
		//获取用户名
		UserIdCause userIdCause = (UserIdCause) this.run.getCause(UserIdCause.class);
		String user_name = userIdCause.getUserName();
		double buildStringTime = run.getStartTimeInMillis();
		String gitlabBranch = null;
		try{
			gitlabBranch = run.getEnvironment(listener).get("gitlabBranch");
		}catch(Exception e){
			e.printStackTrace();
		}
		//拼接通知信息
		String description = "";
		
		Calendar cal = Calendar.getInstance();   
        cal.setTime(d);   
        cal.add(Calendar.HOUR, 8);// 24小时制   
        d = cal.getTime();
        
		if(is_failure == 1){
			//错误报警需要获取错误步骤名称
			description = String.format(text, run.getFullDisplayName(),gitlabBranch,this.stage_name,sdf.format(d),(d.getTime()-buildStringTime)/1000,user_name);
		}else{
			description = String.format(text, run.getFullDisplayName(),gitlabBranch,sdf.format(d),(d.getTime()-buildStringTime)/1000,user_name);
		}
		
		//设置参数
		config.setCorpId(corpId);
		config.setCorpSecret(secret);
		config.setAgentId(Integer.parseInt(agentId));
		article.setTitle(this.title);

		//组装微信推消息的反馈地址
		String url = this.getUrl();
		article.setUrl(url);
		article.setDescription(description);
		listener.getLogger().println(article.toString());
		wxCpService.setWxCpConfigStorage(config);
	}

	/**
	 * Get url string.
	 *
	 * @return the string
	 */
	public String getUrl(){
		
		String fullUrl  = run.getUrl();
		String[] tmp = fullUrl.split("/");
		String projectUrl = tmp[1]+"/"+tmp[2];
		String url = String.format(this.url,tmp[1],projectUrl);
		return url;
	}

	/**
	 * Msg.
	 *
	 * @param tagName the tag name
	 */
	@Autowired
	public  void msg(String tagName) {
	
	
		listener.getLogger().println("开始推送信息到微信");
		
		try {
			String tagId = null;
			

			//根据标签名称获取标签对象
			Optional<WxCpTag> firstTag=wxCpService.tagGet().stream().filter(t->tagName.equals(t.getName())).findFirst();
			
			logger.debug("根据标签名称获取标签:{}",firstTag);
			
			//如果存在此标签则获取标签id
			if (!firstTag.isPresent()) {
				tagId = wxCpService.tagCreate(tagName);
				logger.warn("标签不存在，新建标签id为:{}",tagId);
			}else{
				//如果不存在此标签则创建并获取标签id
				tagId=firstTag.get().getId();
			}
			
			
			logger.debug("标签id为:{}",tagId);
			
			//构建文本消息
			NewsBuilder messageBuilder = WxCpMessage
					.NEWS()
					.agentId(config.getAgentId())
					.addArticle(article);
			
			//企业号可以叠加推送到部门，标签，用户
			
			//推送消息至标签
			listener.getLogger().println("推送到指定标签，标签id为:"+tagId);
			//如果标签下有用户，则推送标签
			messageBuilder.toTag(tagId);
			
			//如果部门id有效，并且部门有人，则推送到部门
			if (StringUtils.isNotBlank(partyId) && !wxCpService.departGetUsers(Integer.parseInt(partyId), true, 1).isEmpty()) {
				listener.getLogger().println("推送到部门，部门id为:"+partyId);
				messageBuilder.toParty(partyId);
			}
			
			//如果用户id有效且用户已关注，则推送到用户
			if (StringUtils.isNotBlank(userId) && checkUserStatus(userId)) {
				listener.getLogger().println("推送到部门，用户id为:"+userId);
				messageBuilder.toUser(userId);
			}
			
			//若部门id、用户id和标签名称皆为空则发送消息至全员。
			if (StringUtils.isBlank(partyId)&&StringUtils.isBlank(userId)&&StringUtils.isBlank(tagName)) {
				listener.getLogger().println("指定的部门，标签，用户 均无效，尝试推送到全部用户");
				messageBuilder.toUser("@all");
			}
			
			//listener.getLogger().println(messageBuilder.build().toJson());
			//推送信息
			wxCpService.messageSend(messageBuilder.build());
			listener.getLogger().println("信息已被推送至微信企业号");
			
		} catch (WxErrorException e) {
			
			ExceptionUtils.getStackTrace(e);
			
			e.printStackTrace();
			listener.getLogger().println("消息推送失败，错误原因："+e.toString());
			
		}
	}
	private boolean checkUserStatus(String userId){
		boolean status = true;
		String[] user_ids = userId.split("\\|");
		for(String user_id : user_ids){
			listener.getLogger().println(user_id);
			try {
				if(wxCpService.userGet(user_id).getStatus() != 1){
					status = false;
				}
			} catch (WxErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return status;
	} 


	
}

package com.killer.weixin;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import hudson.model.Run;
import hudson.model.TaskListener;
import me.chanjar.weixin.common.exception.WxErrorException;
import me.chanjar.weixin.cp.api.WxCpInMemoryConfigStorage;
import me.chanjar.weixin.cp.api.WxCpServiceImpl;
import me.chanjar.weixin.cp.bean.WxCpMessage;
import me.chanjar.weixin.cp.bean.WxCpMessage.WxArticle;
import me.chanjar.weixin.cp.bean.WxCpTag;
import me.chanjar.weixin.cp.bean.messagebuilder.NewsBuilder;

public class WeixinServiceImpl implements WeixinService {

    
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
	
	
	Date d = new Date();
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/**
	 * 设置项目推送的description
	 */
	private String success_context = "%s项目构建成功！结束时间：%s 构建耗时:%f秒";
	private String failure_context = "%s项目构建失败，详细信息请查看Jenkins构建日志！结束时间：%s 构建耗时:%f秒";
	private String aborted_context = "%s项目构建进度被停止，详细信息请查看Jenkins构建日志！结束时间：%s 构建耗时:%f秒";
	
	private WxArticle article = new WxArticle();
	private String url = "http://localhost:8080/%sconsole"; //默认地址
	
	//获取运行数据
	Run<?, ?> run;
	//用来返回运行日志
	TaskListener listener;


    public WeixinServiceImpl(WeixinConnection connection,Run<?, ?> run, TaskListener listener) {
		this.partyId 	= connection.getPartyId();
		this.userId		= connection.getUserId();
		this.tagName	= connection.getTagName();
		
		//根据TokenId获取token信息
		WeixinToken weixinToken = connection.getApiToken(connection.getWeixinTokenId());
		this.corpId 	= weixinToken.getCorpId();
		this.secret 	= weixinToken.getSecret().getPlainText();
		this.agentId 	= weixinToken.getAgentId();
		//判断用户是否设置URL参数
		if(!isNull.equals(connection.getUrl())){//如果Url为不为空，则使用用户设置的地址
			this.url = connection.getUrl()+"/%sconsole";
		}
        this.listener = listener;
        this.run = run;
       
    }


	@Override
    public void success() {
    	//项目构建成功，开始推送信息到微信
		init(success_context);//初始化参数
		msg(tagName);//推送信息到微信
    }
    
   

	@Override
	public void failure() {
		init(failure_context);//初始化参数
		msg(tagName);//推送信息到微信
	}


	@Override
	public void aborted() {
		init(aborted_context);//初始化参数
		msg(tagName);//推送信息到微信
		
	}
	

	public void init(String text){
		
//		listener.getLogger().println("初始化微信企业号配置, 微信企业号:"+this.corpId
//		+",微信企业号密钥:"+this.secret+",应用id:"+this.agentId
//		+"部门id"+this.partyId+"用户id"+this.userId);
		double buildStringTime = run.getStartTimeInMillis();
		//拼接通知信息
		String description = String.format(text, run.getFullDisplayName(),sdf.format(d),(d.getTime()-buildStringTime)/1000);
		
		
		//设置参数
		config.setCorpId(corpId);
		config.setCorpSecret(secret);
		config.setAgentId(Integer.parseInt(agentId));
		article.setTitle(this.title);
		String url = String.format(this.url,run.getUrl());
		article.setUrl(url);
		article.setDescription(description);
		listener.getLogger().println(article.toString());
		wxCpService.setWxCpConfigStorage(config);
	}



	@Autowired
	public  void msg(String tagName) {
	
	
		listener.getLogger().println("开始推送信息到微信");
		
		try {
			String tagId = null;
			
			int flag=0;

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
			
			//标签下无用户，则推送部门或者用户
			if (wxCpService.tagGetUsers(tagId).isEmpty()) {
				
				listener.getLogger().println("该标签下无用户");
				
				//如果部门id有效，并且部门有人，则推送到部门
				if (StringUtils.isNotBlank(partyId) && !wxCpService.departGetUsers(Integer.parseInt(partyId), true, 1).isEmpty()) {
					logger.info("推送到部门，部门id为:{}",partyId);
					messageBuilder.toParty(partyId);
					flag++;
				}
				
				//如果用户id有效且用户已关注，则推送到用户
				if (StringUtils.isNotBlank(userId) && wxCpService.userGet(userId).getStatus() == 1) {
					logger.info("推送到用户，用户id为:{}",userId);
					messageBuilder.toUser(userId);
					flag++;
				}
				
			}else{
				listener.getLogger().println("推送到标签，标签id为:"+tagId);
				//如果标签下有用户，则推送标签
				messageBuilder.toTag(tagId);
				flag++;
			}
			
			if (flag==0) {
				listener.getLogger().println("指定的部门，标签，用户 均无效，尝试推送到全部用户");
				return;
				//messageBuilder.toUser("@all");
			}
			listener.getLogger().println(messageBuilder.build().toJson());
			//推送信息
			wxCpService.messageSend(messageBuilder.build());
			
		} catch (WxErrorException e) {
			
			ExceptionUtils.getStackTrace(e);
			
			e.printStackTrace();
			
		}
	}
	
}

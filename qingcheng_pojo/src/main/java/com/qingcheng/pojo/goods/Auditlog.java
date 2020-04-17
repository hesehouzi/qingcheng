package com.qingcheng.pojo.goods;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
/**
 * auditlog实体类
 * @author Administrator
 *
 */
@Table(name="tb_auditlog")
public class Auditlog implements Serializable{

	@Id
	private String id;//id


	

	private java.util.Date adTime;//ad_time

	private String adPerson;//ad_person

	private String adResult;//ad_result

	private String adSpec;//ad_spec

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public java.util.Date getAdTime() {
		return adTime;
	}
	public void setAdTime(java.util.Date adTime) {
		this.adTime = adTime;
	}

	public String getAdPerson() {
		return adPerson;
	}
	public void setAdPerson(String adPerson) {
		this.adPerson = adPerson;
	}

	public String getAdResult() {
		return adResult;
	}
	public void setAdResult(String adResult) {
		this.adResult = adResult;
	}

	public String getAdSpec() {
		return adSpec;
	}
	public void setAdSpec(String adSpec) {
		this.adSpec = adSpec;
	}


	
}

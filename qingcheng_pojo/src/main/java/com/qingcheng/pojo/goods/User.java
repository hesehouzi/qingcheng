package com.qingcheng.pojo.goods;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
/**
 * user实体类
 * @author Administrator
 *
 */
@Table(name="tb_user")
public class User implements Serializable{

	@Id
	private Long id;//id


	

	private String userName;//用户名

	private String password;//密码

	private String name;//姓名

	private Integer age;//年龄

	private Boolean sex;//性别，1男性，2女性

	private java.util.Date birthday;//出生日期

	private String note;//备注

	private java.util.Date created;//创建时间

	private java.util.Date updated;//更新时间

	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}

	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}

	public Boolean getSex() {
		return sex;
	}
	public void setSex(Boolean sex) {
		this.sex = sex;
	}

	public java.util.Date getBirthday() {
		return birthday;
	}
	public void setBirthday(java.util.Date birthday) {
		this.birthday = birthday;
	}

	public String getNote() {
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}

	public java.util.Date getCreated() {
		return created;
	}
	public void setCreated(java.util.Date created) {
		this.created = created;
	}

	public java.util.Date getUpdated() {
		return updated;
	}
	public void setUpdated(java.util.Date updated) {
		this.updated = updated;
	}


	
}

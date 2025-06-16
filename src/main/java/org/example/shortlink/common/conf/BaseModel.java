package org.example.shortlink.common.conf;

import java.io.Serializable;
import java.util.Date;

public class BaseModel implements Serializable {
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "BaseModel{" +
                "createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}

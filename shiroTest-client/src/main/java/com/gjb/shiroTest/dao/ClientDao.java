package com.gjb.shiroTest.dao;

import com.gjb.shiroTest.entity.Client;
import org.springframework.stereotype.Repository;

/**
 * @author fangxiaobai on 2017/10/13 11:25.
 * @description
 */

@Repository
public interface ClientDao extends BaseDao<Client>{
    
    Object findBySecret(String clientSecret);
    
    Client findByClientId(String clientId);
}

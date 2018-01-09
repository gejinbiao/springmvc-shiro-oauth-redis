package com.gjb.shiroTest.service;

import com.gjb.shiroTest.dao.ClientDao;
import com.gjb.shiroTest.entity.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author fangxiaobai on 2017/10/13 11:28.
 * @description
 */
@Service
public interface ClientService {
    

    public List<Client> findAll();
    
    
    public void save(Client client);
    
    public void delete(Client client);
    
    public Client findByClientId(String clientId);
    
    
    public Object findByClientSecret(String clientSecret);
}

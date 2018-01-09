package com.gjb.shiroTest.dao;

import java.util.List;

/**
 * @author fangxiaobai on 2017/10/13 11:26.
 * @description
 */
public interface BaseDao<T> {
    
    List<T> findAll();
    
    void delete(T t);
    
    void update(T t);
    
    void save(T t);
    
    /**
     * 查询
     * @param id
     * @return T
     */
    T findById(String id);
}

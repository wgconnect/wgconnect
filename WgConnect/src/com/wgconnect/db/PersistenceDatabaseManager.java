/*
 * Copyright 2024 wgconnect@proton.me. All Rights Reserved.
 *
 * WgConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WgConnect.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.db;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * PersistenceDatabaseManager
 *
 * The persistence database manager.
 * 
 * @author: wgconnect@proton.me
 */
public interface PersistenceDatabaseManager {
    
    public void init(Properties properties, List<Class> entityClasses);
            
    public EntityManager getEntityManager();
    public void closeEntityManager();
    
    public void insertEntity(Object entity);
    
    public void updateEntity(Object entity);
  
    public Properties getProperties();
    
    public DataSource getDataSource();
    
    public String getJdbcDriver();
    
    public String getJdbcConnectionUrl();
}

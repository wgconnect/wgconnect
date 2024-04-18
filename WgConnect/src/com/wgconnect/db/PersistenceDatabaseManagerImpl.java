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

import com.mysql.cj.jdbc.MysqlDataSource;

import com.wgconnect.core.util.WgConnectLogger;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * PersistenceDatabaseManager
 *
 * The persistence database manager implementation.
 * 
 * @author: wgconnect@proton.me
 */
public class PersistenceDatabaseManagerImpl implements PersistenceDatabaseManager {
    
    private static final WgConnectLogger log = WgConnectLogger.getLogger(PersistenceDatabaseManagerImpl.class);

    private static final String JDBC_DRIVER = "";
    private static final String URL = "";
    
    private final MysqlDataSource dataSource = new MysqlDataSource();
    private final Properties properties = null;
    private final List<Class> entityClasses = new ArrayList<>();
    
    private EntityManager entityManager = null;

    @Override
    public void init(Properties properties, List<Class> entityClasses) {}

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public void closeEntityManager() {
        if (entityManager != null) {
            entityManager.close();
        }
    }
    
    @Override
    public void insertEntity(Object entity) {}

    @Override
    public void updateEntity(Object entity) {}

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getJdbcDriver() {
        return JDBC_DRIVER;
    }

    @Override
    public String getJdbcConnectionUrl() {
        return URL;
    }
}

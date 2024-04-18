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

import com.wgconnect.db.persistence.PersistenceTunnel;

import java.util.List;
import java.util.Properties;

/**
 * WgDatabaseManager
 *
 * The base class for a database manager.
 * 
 * @author: wgconnect@proton.me
 */
public interface DatabaseManager {
            
    public void init(String databaseFilename, Properties properties);
        
    public void createTables(String... tables);
    
    public void dropTables(String... tables);
        
    public void addTunnels(List<PersistenceTunnel> tunnels);
    
    public void insertTunnel(String tableName, PersistenceTunnel tunnel);
    
    public void updateTunnel(String tableName, PersistenceTunnel tunnel);
  
    public List<PersistenceTunnel> getListFromQuery(String query);
    
    public void start();
    
    public void stop();
    
    public String getDatabaseFilename();

    public String getReleaseVersion();
}

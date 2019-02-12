/**
 * Copyright (C) 2015 Working Group on Joint Research, University Medical Center Mainz
 * Contact: info@osse-register.de
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */

package de.samply.share.broker.utils.db;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.tables.daos.EmailSiteDao;
import de.samply.share.broker.model.db.tables.pojos.EmailSite;

/**
 * This class provides static methods for CRUD operations for EmailSite Objects
 * 
 * @see EmailSite
 */
public final class EmailSiteUtil {
    
    private static final Logger logger = LogManager.getLogger(EmailSiteUtil.class);

    // Prevent instantiation
    private EmailSiteUtil() {
    }

    /**
     * Get the site-association for an email address
     *
     * @param email the email address
     * @return the site-association for the given email address
     */
    public static EmailSite fetchEmailSite(String email) {
        EmailSite emailSite = null;
        Record record;

        try (Connection conn = ResourceManager.getConnection() ) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            record = create.select().from(Tables.EMAIL_SITE).where(Tables.EMAIL_SITE.EMAIL.equalIgnoreCase(email)).fetchOne();
            if (record != null) {
                emailSite = record.into(EmailSite.class);
            }
        } catch (SQLException e) {
            logger.error("SQL Exception caught", e);
        }
        return emailSite;
    }

    /**
     * Delete an email to site association
     *
     * @param emailSite the email to site association to delete
     */
    protected static void deleteEmailSite(EmailSite emailSite) {
        EmailSiteDao emailSiteDao;
        
        try (Connection conn = ResourceManager.getConnection() ) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            emailSiteDao = new EmailSiteDao(configuration);
            
            emailSiteDao.delete(emailSite);
        } catch(SQLException e) {
            logger.error("SQL Exception caught", e);
        }
    }

}

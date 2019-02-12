/**
 * Copyright (C) 2015 Working Group on Joint Research, University Medical Center Mainz
 * Contact: info@osse-register.de
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 * <p>
 * Additional permission under GNU GPL version 3 section 7:
 * <p>
 * If you modify this Program, or any covered work, by linking or combining it
 * with Jersey (https://jersey.java.net) (or a modified version of that
 * library), containing parts covered by the terms of the General Public
 * License, version 2.0, the licensors of this Program grant you additional
 * permission to convey the resulting work.
 */

package de.samply.share.broker.utils.db;

import de.samply.share.broker.jdbc.ResourceManager;
import de.samply.share.broker.model.db.Tables;
import de.samply.share.broker.model.db.enums.DocumentType;
import de.samply.share.broker.model.db.enums.InquiryStatus;
import de.samply.share.broker.model.db.enums.ProjectStatus;
import de.samply.share.broker.model.db.tables.daos.InquiryDao;
import de.samply.share.broker.model.db.tables.daos.UserDao;
import de.samply.share.broker.model.db.tables.pojos.Inquiry;
import de.samply.share.broker.model.db.tables.pojos.Project;
import de.samply.share.broker.model.db.tables.pojos.Site;
import de.samply.share.broker.model.db.tables.pojos.User;
import de.samply.share.common.utils.SamplyShareUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jooq.impl.DSL.select;

/**
 * This class provides static methods for CRUD operations for Inquiry Objects
 *
 * @see Inquiry
 */
public final class InquiryUtil {

    public static final long INQUIRY_TTL = TimeUnit.DAYS.toMillis(28);
    private static final Logger logger = LogManager.getLogger(InquiryUtil.class);

    // Prevent instantiation
    private InquiryUtil() {
    }

    /**
     * Update an inquiry
     *
     * @param inquiry the inquiry to update
     */
    public static void updateInquiry(Inquiry inquiry) {
        InquiryDao inquiryDao;

        try (Connection conn = ResourceManager.getConnection()) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            inquiryDao = new InquiryDao(configuration);
            inquiryDao.update(inquiry);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extend the expiry-date for an inquiry
     *
     * @param inquiryId the id of the inquiry to extend
     */
    public static void extendInquiryById(int inquiryId) {
        InquiryDao inquiryDao;

        try (Connection conn = ResourceManager.getConnection()) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            inquiryDao = new InquiryDao(configuration);
            Inquiry inquiry = inquiryDao.fetchOneById(inquiryId);
            java.sql.Date expiryDate = new java.sql.Date(SamplyShareUtils.getCurrentDate().getTime() + INQUIRY_TTL);
            inquiry.setExpires(expiryDate);
            inquiryDao.update(inquiry);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mark an inquiry as expired by setting the expiry date to yesterday
     *
     * @param inquiryId the id of the inquiry to expire
     */
    public static void expireInquiryById(int inquiryId) {
        InquiryDao inquiryDao;

        try (Connection conn = ResourceManager.getConnection()) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            inquiryDao = new InquiryDao(configuration);
            Inquiry inquiry = inquiryDao.fetchOneById(inquiryId);
            java.sql.Date expiryDate = new java.sql.Date(SamplyShareUtils.getCurrentDate().getTime() - TimeUnit.DAYS.toMillis(1));
            inquiry.setExpires(expiryDate);
            inquiryDao.update(inquiry);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Delete old tentative inquiries.
     *
     * (those that come from central search) older than 1 day
     *
     * @return the number of deleted tentative inquiries
     */
    public static int deleteOldTentativeInquiries() {
        int affectedRows = 0;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            affectedRows = create.delete(Tables.INQUIRY)
                    .where(DSL.currentTimestamp().greaterThan(DSL.timestampAdd(Tables.INQUIRY.CREATED, 1, org.jooq.DatePart.DAY)))
                    .and(Tables.INQUIRY.REVISION.lessThan(1))
                    .and(Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_DRAFT))
                    .execute();
        } catch (SQLException e) {
            logger.error("Caught SQL Exception while trying to delete old tentative inquiries. " + e);
        } catch (DataAccessException dae) {
            logger.error("Caught Data Access Exception while trying to delete old tentative inquiries. " + dae);
        }
        logger.info("Deleted " + affectedRows + " old tentative inquiries");
        return affectedRows;
    }

    /**
     * Delete an inquiry draft.
     *
     * @param inquiry the inquiry to delete
     * @return true on success, false on error
     */
    public static boolean deleteInquiryDraft(Inquiry inquiry) {

        if (inquiry == null) {
            logger.warn("Tried to delete inquiry NULL");
            return false;
        }

        Integer projectId = inquiry.getProjectId();

        if (inquiry.getStatus() != InquiryStatus.IS_DRAFT && !(projectId == null || projectId < 1)) {
            logger.warn("Tried to delete an inquiry that is either not a draft or is associated with a project. Deny it.");
            return false;
        }

        deleteInquirySiteConnection(inquiry);
        deleteExposeForInquiry(inquiry);

        InquiryDao inquiryDao;

        try (Connection conn = ResourceManager.getConnection()) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            inquiryDao = new InquiryDao(configuration);
            inquiryDao.delete(inquiry);
        } catch (SQLException e) {
            logger.error("SQL Exception caught while trying to delete inquiry draft.", e);
            return false;
        }
        return true;
    }

    /**
     * Delete all inquiry site connections for an inquiry
     *
     * @param inquiry the inquiry to delete the sites from
     */
    private static void deleteInquirySiteConnection(Inquiry inquiry) {
        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            create.delete(Tables.INQUIRY_SITE)
                    .where(Tables.INQUIRY_SITE.INQUIRY_ID
                            .equal(inquiry.getId()))
                    .execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if an inquiry has an expose attached to it
     *
     * @param inquiryId the id of the inquiry to check
     * @return true if an expose is linked with this inquiry, false otherwise
     */
    public static boolean inquiryHasExpose(int inquiryId) {
        int count = 0;
        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext dslContext = ResourceManager.getDSLContext(conn);

            count = dslContext.fetchCount(Tables.DOCUMENT,
                    (Tables.DOCUMENT.INQUIRY_ID).equal(inquiryId)
                            .and(Tables.DOCUMENT.DOCUMENT_TYPE.equal(DocumentType.DT_EXPOSE))
            );

        } catch (SQLException e) {
            logger.error("Error trying to check if inquiry " + inquiryId + " has an expose");
        }
        return (count > 0);
    }

    /**
     * Get all inquiry drafts for a user
     *
     * Ordered by creation date in descending order
     *
     * @param userId the id of the user whose inquiry drafts shall be loaded
     * @return a list of inquiry drafts
     */
    public static List<Inquiry> fetchInquiryDraftsFromUserOrderByDate(int userId) {
        List<Inquiry> inquiries = null;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            inquiries = create.select()
                    .from(Tables.INQUIRY)
                    .where(
                            Tables.INQUIRY.AUTHOR_ID.equal(userId)
                                    .and(Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_DRAFT))
                    )
                    .orderBy(Tables.INQUIRY.CREATED.desc())
                    .fetchInto(Inquiry.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiries;
    }

    /**
     * Get all released inquiries for a user
     *
     * Ordered by creation date in descending order
     *
     * @param userId the id of the user whose released inquiries shall be loaded
     * @return a list of released inquiries
     */
    public static List<Inquiry> fetchReleasedInquiriesFromUserOrderByDate(int userId) {
        List<Inquiry> inquiries = null;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            inquiries = create.select()
                    .from(Tables.INQUIRY)
                    .where(
                            Tables.INQUIRY.AUTHOR_ID.equal(userId)
                                    .and(Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_RELEASED))
                                    .and(Tables.INQUIRY.EXPIRES.greaterOrEqual(SamplyShareUtils.getCurrentDate()))
                                    .and(Tables.INQUIRY.PROJECT_ID.isNull())
                    )
                    .orderBy(Tables.INQUIRY.CREATED.desc())
                    .fetchInto(Inquiry.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiries;
    }

    /**
     * Get all released inquiries, that also have projects linked to them, for a user
     *
     * Ordered by creation date in descending order
     *
     * @param userId the id of the user whose released inquiries shall be loaded
     * @return a list of released inquiries
     */
    public static List<Inquiry> fetchReleasedInquiriesWithProjectsFromUserOrderByDate(int userId) {
        List<Inquiry> inquiries = null;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            inquiries = create.select()
                    .from(Tables.INQUIRY)
                    .where(
                            Tables.INQUIRY.AUTHOR_ID.equal(userId)
                                    .and(Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_RELEASED))
                                    .and(Tables.INQUIRY.EXPIRES.greaterOrEqual(SamplyShareUtils.getCurrentDate()))
                                    .and(Tables.INQUIRY.PROJECT_ID.isNotNull())
                    )
                    .orderBy(Tables.INQUIRY.CREATED.desc())
                    .fetchInto(Inquiry.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiries;
    }

    /**
     * Get all archived inquiries for a user
     *
     * Ordered by creation date in descending order
     *
     * @param userId the id of the user whose archived inquiries shall be loaded
     * @return a list of archived inquiries
     */
    public static List<Inquiry> fetchArchivedInquiriesFromUserOrderByDate(int userId) {
        List<Inquiry> inquiries = null;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            inquiries = create.select()
                    .from(Tables.INQUIRY)
                    .where(
                            Tables.INQUIRY.AUTHOR_ID.equal(userId)
                                    .and(Tables.INQUIRY.STATUS.notEqual(InquiryStatus.IS_DRAFT))
                                    .and(
                                            Tables.INQUIRY.EXPIRES.lessThan(SamplyShareUtils.getCurrentDate())
                                                    .or(Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_OUTDATED))
                                    )

                    )
                    .orderBy(Tables.INQUIRY.CREATED.desc())
                    .fetchInto(Inquiry.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiries;
    }

    /**
     * Get the user that created the inquiry
     *
     * @param inquiry the inquiry for which the user is wanted
     * @return the user that created the inquiry
     */
    public static User getUserForInquiry(Inquiry inquiry) {
        User user = null;
        UserDao userDao;

        try (Connection conn = ResourceManager.getConnection()) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            userDao = new UserDao(configuration);
            user = userDao.fetchOneById(inquiry.getAuthorId());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    /**
     * Count how many inquiries with a given status the user has
     *
     * @param inquiryStatus the status of the inquiries to count
     * @param userId the id of the user whose inquiries shall be counted
     * @return the amount of inquiries with the given status, created by this user
     */
    public static Integer countInquiries(InquiryStatus inquiryStatus, int userId) {
        Integer count = 0;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);
            if (inquiryStatus.equals(InquiryStatus.IS_OUTDATED)) {
                count = create.fetchCount(Tables.INQUIRY,
                        (Tables.INQUIRY.STATUS.equal(inquiryStatus)
                                .or(Tables.INQUIRY.EXPIRES.lessThan(SamplyShareUtils.getCurrentDate())))
                        .and(Tables.INQUIRY.AUTHOR_ID.equal(userId)));
            } else if (inquiryStatus.equals(InquiryStatus.IS_DRAFT)) {
                count = create.fetchCount(Tables.INQUIRY,
                        Tables.INQUIRY.STATUS.equal(inquiryStatus)
                                .and(Tables.INQUIRY.AUTHOR_ID.equal(userId)));
            } else {
                count = create.fetchCount(Tables.INQUIRY,
                        Tables.INQUIRY.STATUS.equal(inquiryStatus)
                                .and(Tables.INQUIRY.EXPIRES.greaterOrEqual(SamplyShareUtils.getCurrentDate()))
                                .and(Tables.INQUIRY.AUTHOR_ID.equal(userId)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Count the amount of released inquiries a user has
     *
     * @param withProject if set to true, only the inquiries are counted, that are linked with a project
     * @param userId the id of the user whose released inquiries shall be counted
     * @return the amount of released inquiries, created by this user
     */
    public static Integer countReleasedInquiries(boolean withProject, int userId) {
        Integer count = 0;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);
            if (withProject) {
                count = create.fetchCount(Tables.INQUIRY,
                        Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_RELEASED)
                                .and(Tables.INQUIRY.AUTHOR_ID.equal(userId))
                                .and(Tables.INQUIRY.PROJECT_ID.isNotNull())
                                .and(Tables.INQUIRY.EXPIRES.greaterOrEqual(SamplyShareUtils.getCurrentDate())));
            } else {
                count = create.fetchCount(Tables.INQUIRY,
                        Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_RELEASED)
                                .and(Tables.INQUIRY.AUTHOR_ID.equal(userId))
                                .and(Tables.INQUIRY.PROJECT_ID.isNull())
                                .and(Tables.INQUIRY.EXPIRES.greaterOrEqual(SamplyShareUtils.getCurrentDate())));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * Fetch an inquiry by its id
     *
     * @param inquiryId the id of the inquiry to fetch
     * @return the inquiry
     */
    public static Inquiry fetchInquiryById(int inquiryId) {
        Inquiry inquiry = null;
        InquiryDao inquiryDao;

        try (Connection conn = ResourceManager.getConnection()) {
            Configuration configuration = new DefaultConfiguration().set(conn).set(SQLDialect.POSTGRES);
            inquiryDao = new InquiryDao(configuration);
            inquiry = inquiryDao.fetchOneById(inquiryId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiry;
    }

    /**
     * Get all inquiries for a site
     *
     * Only include released inquiries that are not expired
     *
     * @param siteId the id of the site for which the inquiries shall be loaded
     * @return a list of all active inquiries for this site
     */
    public static List<Inquiry> fetchInquiriesForSite(int siteId) {
        List<Inquiry> inquiries = null;

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            inquiries = create.select(Tables.INQUIRY.fields())
                    .from(Tables.INQUIRY.join(Tables.INQUIRY_SITE).onKey().join(Tables.SITE).onKey().leftOuterJoin(Tables.PROJECT).onKey())
                    .where((Tables.SITE.ID).equal(siteId)
                            .and(Tables.INQUIRY.EXPIRES.greaterOrEqual(SamplyShareUtils.getCurrentDate()))
                            .and(Tables.INQUIRY.STATUS.equal(InquiryStatus.IS_RELEASED))
                            .and(
                                    Tables.PROJECT.ID.isNull()
                                            .or(Tables.PROJECT.STATUS.equal(ProjectStatus.PS_OPEN_DISTRIBUTION))
                            ))
                    .fetchInto(Inquiry.class);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiries;
    }

    /**
     * Get all project partners for an inquiry (via the project this inquiry belongs to)
     *
     * @param inquiryId the id of the inquiry
     * @return a list of partner sites for the project that contains this inquiry
     */
    public static List<Site> fetchPartnersForInquiry(int inquiryId) {
        Project project = ProjectUtil.fetchProjectByInquiryId(inquiryId);
        return ProjectUtil.fetchProjectPartners(project);
    }

    /**
     * Delete the expose belonging to an inquiry
     *
     * @param inquiry the inquiry whose expose will be deleted
     */
    private static void deleteExposeForInquiry(Inquiry inquiry) {
        if (inquiry != null) {
            DocumentUtil.deleteExposeByInquiryId(inquiry.getId());
        }
    }

    /**
     * Get a list of inquiries that will expire in a given amount of days
     *
     * @param days the threshold for inquiry expiration in days
     * @return a list of inquiries that will expire in the given amount of days
     */
    public static List<Inquiry> getInquiriesThatExpireInDays(int days) {
        List<Inquiry> inquiries = null;
        java.sql.Date threshold = new java.sql.Date(SamplyShareUtils.getCurrentDate().getTime() + TimeUnit.DAYS.toMillis(days));

        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);

            inquiries = create.select(Tables.INQUIRY.fields())
                    .from(Tables.INQUIRY)
                    .where(Tables.INQUIRY.EXPIRES.equal(threshold))
                    .fetchInto(Inquiry.class);

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return inquiries;
    }

    public static void deleteSimpleResultInquiry(){
        int affectedRows = 0;
        //TODO: if necessary change ID from user? The searchbroker user in the DB has the ID 1
        try (Connection conn = ResourceManager.getConnection()) {
            DSLContext create = ResourceManager.getDSLContext(conn);
            affectedRows = create.delete(Tables.INQUIRY_SITE)
                    .where(Tables.INQUIRY_SITE.INQUIRY_ID.in(
                            select(Tables.INQUIRY.ID)
                                    .from(Tables.INQUIRY)
                                    .where(DSL.currentTimestamp().greaterThan(DSL.timestampAdd(Tables.INQUIRY.CREATED, 1, org.jooq.DatePart.DAY)))
                                    .and(Tables.INQUIRY.AUTHOR_ID.eq(1))
                    ))
                    .execute();
            affectedRows = create.delete(Tables.DOCUMENT)
                    .where(Tables.DOCUMENT.INQUIRY_ID.in(
                            select(Tables.INQUIRY.ID)
                                    .from(Tables.INQUIRY)
                                    .where(DSL.currentTimestamp().greaterThan(DSL.timestampAdd(Tables.INQUIRY.CREATED, 1, org.jooq.DatePart.DAY)))
                                    .and(Tables.INQUIRY.AUTHOR_ID.eq(1))
                    ))
                    .execute();
            affectedRows = create.delete(Tables.REPLY)
                    .where(Tables.REPLY.INQUIRY_ID.in(
                            select(Tables.INQUIRY.ID)
                                    .from(Tables.INQUIRY)
                                    .where(DSL.currentTimestamp().greaterThan(DSL.timestampAdd(Tables.INQUIRY.CREATED, 1, org.jooq.DatePart.DAY)))
                                    .and(Tables.INQUIRY.AUTHOR_ID.eq(1))
                    ))
                    .execute();
            affectedRows = create.delete(Tables.INQUIRY)
                    .where(DSL.currentTimestamp().greaterThan(DSL.timestampAdd(Tables.INQUIRY.CREATED, 1, org.jooq.DatePart.DAY)))
                    .and(Tables.INQUIRY.AUTHOR_ID.eq(1))
                    .execute();
        } catch (SQLException e) {
            logger.error("Caught SQL Exception while trying to delete old inquiries. " + e);
        } catch (DataAccessException dae) {
            logger.error("Caught Data Access Exception while trying to delete old inquiries. " + dae);
        }
        logger.info("Deleted " + affectedRows + " old  inquiries");
    }
}

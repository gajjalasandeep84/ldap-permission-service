package com.example.useraccess.repository;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
@Profile("db")
public class UserPermissionRepositoryImpl implements UserPermissionRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<String> getDirectPermissions(String userId) {
		String sql = """
				    SELECT DISTINCT permission
				    FROM (
				        SELECT bf.FUNCTION_NAME || '_READ' AS permission
				        FROM BO_USER_FUNCTION_MPNG bufm
				        JOIN BO_FUNCTION bf ON bf.ID = bufm.BO_FUNCTION_ID
				        WHERE bufm.USER_ID = :userId
				          AND bufm.READ_ACCESS_IND = 'Y'

				        UNION

				        SELECT bf.FUNCTION_NAME || '_UPDATE'
				        FROM BO_USER_FUNCTION_MPNG bufm
				        JOIN BO_FUNCTION bf ON bf.ID = bufm.BO_FUNCTION_ID
				        WHERE bufm.USER_ID = :userId
				          AND bufm.UPDATE_ACCESS_IND = 'Y'
				    ) permissions
				    ORDER BY permission
				""";

		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("userId", userId);
		return query.getResultList();
	}

	@Override
	public int getDirectPermissionCount(String userId) {
		String sql = """
				    SELECT COUNT(*)
				    FROM (
				        SELECT bf.FUNCTION_NAME || '_READ' AS permission
				        FROM BO_USER_FUNCTION_MPNG bufm
				        JOIN BO_FUNCTION bf ON bf.ID = bufm.BO_FUNCTION_ID
				        WHERE bufm.USER_ID = :userId
				          AND bufm.READ_ACCESS_IND = 'Y'

				        UNION

				        SELECT bf.FUNCTION_NAME || '_UPDATE'
				        FROM BO_USER_FUNCTION_MPNG bufm
				        JOIN BO_FUNCTION bf ON bf.ID = bufm.BO_FUNCTION_ID
				        WHERE bufm.USER_ID = :userId
				          AND bufm.UPDATE_ACCESS_IND = 'Y'
				    ) permissions
				""";

		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("userId", userId);

		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}
}
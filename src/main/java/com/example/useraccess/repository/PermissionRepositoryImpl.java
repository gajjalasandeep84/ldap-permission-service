package com.example.useraccess.repository;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
@Profile("db")
public class PermissionRepositoryImpl implements PermissionRepositoryCustom {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public int getEffectivePermissionCount(String userId, List<String> roles) {

		String sql = """
				    SELECT COUNT(*)
				    FROM (
				        SELECT bf.FUNCTION_NAME || '_READ' AS permission
				        FROM BO_ROLE r
				        JOIN BO_ROLE_FUNCTION_MPNG mp ON mp.ROLE_ID = r.ID
				        JOIN BO_FUNCTION bf ON bf.ID = mp.FUNCTION_ID
				        WHERE r.ROLE_NAME IN (:roles)
				          AND mp.READ_ACCESS_IND = 'Y'

				        UNION

				        SELECT bf.FUNCTION_NAME || '_UPDATE'
				        FROM BO_ROLE r
				        JOIN BO_ROLE_FUNCTION_MPNG mp ON mp.ROLE_ID = r.ID
				        JOIN BO_FUNCTION bf ON bf.ID = mp.FUNCTION_ID
				        WHERE r.ROLE_NAME IN (:roles)
				          AND mp.UPDATE_ACCESS_IND = 'Y'

				        UNION

				        SELECT bf.FUNCTION_NAME || '_READ'
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
		query.setParameter("roles", roles);

		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getEffectivePermissions(String userId, List<String> roles) {

		String sql = """
				    SELECT DISTINCT permission
				    FROM (
				        SELECT bf.FUNCTION_NAME || '_READ' AS permission
				        FROM BO_ROLE r
				        JOIN BO_ROLE_FUNCTION_MPNG mp ON mp.ROLE_ID = r.ID
				        JOIN BO_FUNCTION bf ON bf.ID = mp.FUNCTION_ID
				        WHERE r.ROLE_NAME IN (:roles)
				          AND mp.READ_ACCESS_IND = 'Y'

				        UNION

				        SELECT bf.FUNCTION_NAME || '_UPDATE'
				        FROM BO_ROLE r
				        JOIN BO_ROLE_FUNCTION_MPNG mp ON mp.ROLE_ID = r.ID
				        JOIN BO_FUNCTION bf ON bf.ID = mp.FUNCTION_ID
				        WHERE r.ROLE_NAME IN (:roles)
				          AND mp.UPDATE_ACCESS_IND = 'Y'

				        UNION

				        SELECT bf.FUNCTION_NAME || '_READ'
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
		query.setParameter("roles", roles);

		return query.getResultList();
	}
}
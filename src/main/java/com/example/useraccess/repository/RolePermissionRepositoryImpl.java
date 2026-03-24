package com.example.useraccess.repository;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
@Profile("db")
public class RolePermissionRepositoryImpl implements RolePermissionRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<String> getPermissionsByRoles(List<String> roles) {
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
				    ) permissions
				    ORDER BY permission
				""";

		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("roles", roles);
		return query.getResultList();
	}

	@Override
	public int getPermissionCountByRoles(List<String> roles) {
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
				    ) permissions
				""";

		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("roles", roles);

		Number result = (Number) query.getSingleResult();
		return result.intValue();
	}
}
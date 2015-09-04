package com.sohlman.liferay.validate.service;

import com.liferay.portal.service.ServiceWrapper;

/**
 * Provides a wrapper for {@link ValidateService}.
 *
 * @author Brian Wing Shun Chan
 * @see ValidateService
 * @generated
 */
public class ValidateServiceWrapper implements ValidateService,
    ServiceWrapper<ValidateService> {
    private ValidateService _validateService;

    public ValidateServiceWrapper(ValidateService validateService) {
        _validateService = validateService;
    }

    /**
    * Returns the Spring bean ID for this bean.
    *
    * @return the Spring bean ID for this bean
    */
    @Override
    public java.lang.String getBeanIdentifier() {
        return _validateService.getBeanIdentifier();
    }

    /**
    * Sets the Spring bean ID for this bean.
    *
    * @param beanIdentifier the Spring bean ID for this bean
    */
    @Override
    public void setBeanIdentifier(java.lang.String beanIdentifier) {
        _validateService.setBeanIdentifier(beanIdentifier);
    }

    @Override
    public java.lang.Object invokeMethod(java.lang.String name,
        java.lang.String[] parameterTypes, java.lang.Object[] arguments)
        throws java.lang.Throwable {
        return _validateService.invokeMethod(name, parameterTypes, arguments);
    }

    @Override
    public void validateAllArticles() {
        _validateService.validateAllArticles();
    }

    /**
     * @deprecated As of 6.1.0, replaced by {@link #getWrappedService}
     */
    public ValidateService getWrappedValidateService() {
        return _validateService;
    }

    /**
     * @deprecated As of 6.1.0, replaced by {@link #setWrappedService}
     */
    public void setWrappedValidateService(ValidateService validateService) {
        _validateService = validateService;
    }

    @Override
    public ValidateService getWrappedService() {
        return _validateService;
    }

    @Override
    public void setWrappedService(ValidateService validateService) {
        _validateService = validateService;
    }
}

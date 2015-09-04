package com.sohlman.liferay.validate.service.impl;

import com.liferay.portal.LocaleException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.lar.ExportImportThreadLocal;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.LocalizationUtil;
import com.liferay.portal.kernel.util.PrefsPropsUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.model.Image;
import com.liferay.portal.service.ImageLocalServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.dynamicdatamapping.NoSuchTemplateException;
import com.liferay.portlet.dynamicdatamapping.StorageFieldNameException;
import com.liferay.portlet.dynamicdatamapping.StorageFieldRequiredException;
import com.liferay.portlet.dynamicdatamapping.model.DDMStructure;
import com.liferay.portlet.dynamicdatamapping.model.DDMTemplate;
import com.liferay.portlet.dynamicdatamapping.service.DDMStructureLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.service.DDMTemplateLocalServiceUtil;
import com.liferay.portlet.dynamicdatamapping.storage.Fields;
import com.liferay.portlet.dynamicdatamapping.util.DDMXMLUtil;
import com.liferay.portlet.journal.ArticleContentException;
import com.liferay.portlet.journal.ArticleExpirationDateException;
import com.liferay.portlet.journal.ArticleIdException;
import com.liferay.portlet.journal.ArticleSmallImageSizeException;
import com.liferay.portlet.journal.ArticleTitleException;
import com.liferay.portlet.journal.ArticleTypeException;
import com.liferay.portlet.journal.model.JournalArticle;
import com.liferay.portlet.journal.model.JournalArticleConstants;
import com.liferay.portlet.journal.service.JournalArticleLocalServiceUtil;
import com.sohlman.liferay.validate.service.base.ValidateServiceBaseImpl;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The implementation of the validate remote service.
 *
 * <p>
 * All custom service methods should be put in this class. Whenever methods are added, rerun ServiceBuilder to copy their definitions into the {@link com.sohlman.liferay.validate.service.ValidateService} interface.
 *
 * <p>
 * This is a remote service. Methods of this service are expected to have security checks based on the propagated JAAS credentials because this service can be accessed remotely.
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see com.sohlman.liferay.validate.service.base.ValidateServiceBaseImpl
 * @see com.sohlman.liferay.validate.service.ValidateServiceUtil
 */
public class ValidateServiceImpl extends ValidateServiceBaseImpl {
    /*
     * NOTE FOR DEVELOPERS:
     *
     * Never reference this interface directly. Always use {@link com.sohlman.liferay.validate.service.ValidateServiceUtil} to access the validate remote service.
     */
	
	public void validateAllArticles() {

		try {
			List<JournalArticle> list = JournalArticleLocalServiceUtil
					.getJournalArticles(-1, -1);

			for (JournalArticle journalArticle : list) {

				try {
					Image image = ImageLocalServiceUtil.getImage(journalArticle
							.getSmallImageId());
					validate(journalArticle.getCompanyId(),
							journalArticle.getGroupId(),
							journalArticle.getClassNameId(),
							journalArticle.getTitleMap(),
							journalArticle.getContent(),
							journalArticle.getType(),
							journalArticle.getStructureId(),
							journalArticle.getTemplateId(),
							journalArticle.getExpirationDate(),
							journalArticle.getSmallImage(),
							journalArticle.getSmallImageURL(),
							image == null ? null : image.getTextObj());
					// Uncomment this if you want to list all articles to be scanned
					// _log.error(String.format("OK Article : groupID:%d articleId:%s version:%f ", journalArticle.getGroupId(), journalArticle.getArticleId(), journalArticle.getVersion()));
				} catch (PortalException pe) {
					_log.error(String.format("ArticleValidationError : groupID:%d articleId:%s version:%f ", journalArticle.getGroupId(), journalArticle.getArticleId(), journalArticle.getVersion()));
				}
			}

		} catch (SystemException e) {
			_log.error(e);
		}
	}
	
	
	protected void validate(String articleId) throws PortalException {
		if (Validator.isNull(articleId)
				|| (articleId.indexOf(CharPool.COMMA) != -1)
				|| (articleId.indexOf(CharPool.SPACE) != -1)) {

			throw new ArticleIdException();
		}
	}

	protected void validate(long companyId, long groupId, long classNameId,
			Map<Locale, String> titleMap, String content, String type,
			String ddmStructureKey, String ddmTemplateKey, Date expirationDate,
			boolean smallImage, String smallImageURL, byte[] smallImageBytes)
			throws PortalException, SystemException {

		Locale articleDefaultLocale = LocaleUtil
				.fromLanguageId(LocalizationUtil.getDefaultLanguageId(content));

		Locale[] availableLocales = LanguageUtil.getAvailableLocales(groupId);

		if (!ArrayUtil.contains(availableLocales, articleDefaultLocale)) {
			LocaleException le = new LocaleException(
					LocaleException.TYPE_CONTENT, "The locale "
							+ articleDefaultLocale
							+ " is not available in site with groupId"
							+ groupId);

			Locale[] sourceAvailableLocales = { articleDefaultLocale };

			le.setSourceAvailableLocales(sourceAvailableLocales);
			le.setTargetAvailableLocales(availableLocales);

			throw le;
		}

		if ((classNameId == JournalArticleConstants.CLASSNAME_ID_DEFAULT)
				&& (titleMap.isEmpty() || Validator.isNull(titleMap
						.get(articleDefaultLocale)))) {

			throw new ArticleTitleException();
		} else if (Validator.isNull(type)) {
			throw new ArticleTypeException();
		}

		validateContent(content);
		
		if (Validator.isNotNull(ddmStructureKey)) {
			DDMStructure ddmStructure = DDMStructureLocalServiceUtil
					.getStructure(groupId,
							PortalUtil.getClassNameId(JournalArticle.class),
							ddmStructureKey, true);

			validateDDMStructureFields(ddmStructure, classNameId, content);

			if (Validator.isNotNull(ddmTemplateKey)) {
				DDMTemplate ddmTemplate = DDMTemplateLocalServiceUtil
						.getTemplate(groupId,
								PortalUtil.getClassNameId(DDMStructure.class),
								ddmTemplateKey, true);

				if (ddmTemplate.getClassPK() != ddmStructure.getStructureId()) {
					throw new NoSuchTemplateException("{templateKey="
							+ ddmTemplateKey + "}");
				}
			} else if (classNameId == JournalArticleConstants.CLASSNAME_ID_DEFAULT) {

				throw new NoSuchTemplateException();
			}
		}

		if ((expirationDate != null) && expirationDate.before(new Date())
				&& !ExportImportThreadLocal.isImportInProcess()) {

			throw new ArticleExpirationDateException();
		}
/*
		String[] imageExtensions = PrefsPropsUtil.getStringArray(
				PropsKeys.JOURNAL_IMAGE_EXTENSIONS, StringPool.COMMA);

		long smallImageMaxSize = PrefsPropsUtil
				.getLong(PropsKeys.JOURNAL_IMAGE_SMALL_MAX_SIZE);

		if ((smallImageMaxSize > 0)
				&& ((smallImageBytes == null) || (smallImageBytes.length > smallImageMaxSize))) {

			throw new ArticleSmallImageSizeException();
		}
*/
	}

	protected void validateContent(String content) throws PortalException {
		if (Validator.isNull(content)) {
			throw new ArticleContentException("Content is null");
		}

		try {
			SAXReaderUtil.read(content);
		} catch (DocumentException de) {
			if (_log.isDebugEnabled()) {
				_log.debug("Invalid content:\n" + content);
			}

			throw new ArticleContentException(
					"Unable to read content with an XML parser", de);
		}
	}

	protected void validateDDMStructureFields(DDMStructure ddmStructure,
			long classNameId, Fields fields) throws PortalException,
			SystemException {

		for (com.liferay.portlet.dynamicdatamapping.storage.Field field : fields) {

			if (!ddmStructure.hasField(field.getName())) {
				throw new StorageFieldNameException();
			}

			if (ddmStructure.getFieldRequired(field.getName())
					&& Validator.isNull(field.getValue())
					&& (classNameId == JournalArticleConstants.CLASSNAME_ID_DEFAULT)) {

				throw new StorageFieldRequiredException();
			}
		}
	}

	protected void validateDDMStructureFields(DDMStructure ddmStructure,
			long classNameId, String content) throws PortalException,
			SystemException {

		Fields fields = DDMXMLUtil.getFields(ddmStructure, content);

		validateDDMStructureFields(ddmStructure, classNameId, fields);
	}

	private static Log _log = LogFactoryUtil.getLog(ValidateServiceImpl.class);	
}

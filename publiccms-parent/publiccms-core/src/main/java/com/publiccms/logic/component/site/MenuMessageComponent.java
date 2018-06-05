package com.publiccms.logic.component.site;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractMessageSource;
import org.springframework.stereotype.Component;

import com.publiccms.common.api.Cache;
import com.publiccms.common.cache.CacheEntity;
import com.publiccms.common.cache.CacheEntityFactory;
import com.publiccms.common.constants.CommonConstants;
import com.publiccms.common.tools.CommonUtils;
import com.publiccms.entities.sys.SysModuleLang;
import com.publiccms.logic.service.sys.SysModuleLangService;

/**
 * MenuMessageComponent 菜单国际化组件
 *
 */
@Component
public class MenuMessageComponent extends AbstractMessageSource implements Cache {
    public static final String CODE = "menu";
    public static final String PREFIX = CODE + CommonConstants.DOT;
    private MessageSourceControl messageSourceControl = new MessageSourceControl();

    private CacheEntity<String, Map<String, String>> messageCache;
    private Map<String, Map<Locale, MessageFormat>> messageFormatCaches = new HashMap<>();

    @Autowired
    private SysModuleLangService sysModuleLangService;

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        Map<Locale, MessageFormat> messageFormatMap = messageFormatCaches.get(code);
        if (null == messageFormatMap) {
            messageFormatMap = new HashMap<>();
            MessageFormat messageFormat = messageFormatMap.get(locale);
            if (null == messageFormat) {
                String message = resolveCodeWithoutArguments(code, locale);
                if (null != message) {
                    messageFormat = createMessageFormat(message, locale);
                    messageFormatMap.put(locale, messageFormat);
                    return messageFormat;
                }
            }
            messageFormatCaches.put(code, messageFormatMap);
        }
        return null;
    }

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        Map<String, String> map = getMessageMap(locale);
        if (null != map) {
            String message = map.get(code);
            if (null == message) {
                List<Locale> candidateLocales = messageSourceControl.getCandidateLocales(CODE, locale);
                for (Locale l : candidateLocales) {
                    if (!l.equals(locale)) {
                        Map<String, String> messageMap = getMessageMap(l);
                        if (null != messageMap) {
                            message = messageMap.get(code);
                            if (null != message) {
                                return message;
                            }
                        }
                    }
                }
                return null;
            } else {
                return message;
            }
        } else {
            return null;
        }
    }

    private Map<String, String> getMessageMap(Locale locale) {
        String lang = locale.toString();
        Map<String, String> messageMap = messageCache.get(lang);
        if (null == messageMap) {
            synchronized (messageCache) {
                messageMap = messageCache.get(lang);
                if (null == messageMap) {
                    messageMap = new HashMap<>();
                    @SuppressWarnings("unchecked")
                    List<SysModuleLang> list = (List<SysModuleLang>) sysModuleLangService.getList(null, lang);
                    if (CommonUtils.notEmpty(list)) {
                        for (SysModuleLang entity : list) {
                            messageMap.put(PREFIX + entity.getId().getModuleId(), entity.getValue());
                        }
                    }
                }
                messageCache.put(lang, messageMap);
            }
        }
        return messageMap;
    }

    /**
     * @param cacheEntityFactory
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    @Autowired
    public void initCache(CacheEntityFactory cacheEntityFactory)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        messageCache = cacheEntityFactory.createCacheEntity(CODE);
    }

    @Override
    public void clear() {
        messageCache.clear();
        messageFormatCaches.clear();
    }

    private class MessageSourceControl extends ResourceBundle.Control {
    }
}
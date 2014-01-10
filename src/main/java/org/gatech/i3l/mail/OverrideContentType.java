package org.gatech.i3l.mail;

import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;

/**
 * <p>Overrides Content-Type's primary and sub type values for all Emails with matched
 * attachment. Note that if Content-Type's Primary/Sub type is changed improperly, client
 * application may behave incorrectly. So, this must be used with care.</p>
 * 
 * <pre>
 *   &lt;mailet match=&quot;All&quot; class=&quot;org.gatech.i3l.mail.OverrideContentType&quot;&gt;
 *     &lt;filePattern &gt;.*\.xml&lt;/filePattern&gt;  &lt;!-- Regular expression that must be matched to filename --&gt;
 *     &lt;subTypePattern&gt;[xX][mM][lL]&lt;/subTypePattern&gt;  &lt;!-- Regular express of SubType that will be ignored by this Mailet --&gt;
 *     &lt;primeType&gt;text&lt;/primeType&gt; &lt;!-- Prime Type to be replaced with --&gt;
 *     &lt;subType&gt;xml&lt;/subType&gt; &lt;!-- Sub Type to be replaced with --&gt;
 *   &lt;/mailet &gt;
 * </pre>
 * 
 */
public class OverrideContentType extends GenericMailet {

    public static final String FILE_PATTERN_NAME = "filePattern";
    public static final String SUBTYPE_PATTERN_NAME = "subTypePattern";
    public static final String PRIMETYPE_NAME = "primeType";
    public static final String SUBTYPE_NAME = "subType";
    private String subTypePatternName = null;
    private String PrimeTypeName = null;
    private String SubTypeName = null;
    private Pattern regExPattern = null;

    /**
     * Checks if the mandatory parameters are present.
     * FilePattern, PrimeTypeName, and SubTypeName must be present. 
     * @throws MailetException
     */
    public void init() throws MailetException {
        String FilePattern = getInitParameter(FILE_PATTERN_NAME);
        subTypePatternName = getInitParameter(SUBTYPE_PATTERN_NAME);
        PrimeTypeName = getInitParameter(PRIMETYPE_NAME);
        SubTypeName = getInitParameter(SUBTYPE_NAME);

        if (FilePattern == null) {
            throw new MailetException("No values for " + FILE_PATTERN_NAME
                    + " parameter was provided.");
        }

        if (PrimeTypeName == null) {
            throw new MailetException("No value for " + PRIMETYPE_NAME
                    + " parameter was provided.");
        }
        
        if (SubTypeName == null) {
            throw new MailetException("No value for " + SUBTYPE_NAME
                    + " parameter was provided.");
        }

        try {
            regExPattern = Pattern.compile(FilePattern);
        } catch (Exception e) {
            throw new MailetException("Could not compile regex ["
                    + FilePattern + "].");
        }

    }

    /**
     * Service the mail: evaluate contents with attachment. Compare with input
     *   file pattern. If matched, overwrite Content-Type's PrimaryType/SubType.
     * 
     * @param mail
     *            The mail to service
     * @throws MailetException
     *             Thrown when an error situation is encountered.
     */
    public void service(Mail mail) throws MailetException {
        MimeMessage message;
        try {
            message = mail.getMessage();
        } catch (MessagingException e) {
            throw new MailetException(
                    "Could not retrieve message from Mail object", e);
        }
        // All MIME messages with an attachment are multipart, so we do nothing
        // if it is not mutlipart
        try {
            if (message.isMimeType("multipart/*")) {
            	evalMessage(message, mail);
            }
        } catch (MessagingException e) {
            throw new MailetException("Could not retrieve contenttype of message.", e);
        } catch (Exception e) {
            throw new MailetException("Could not evaluate message.", e);
        }
    }

    /**
     * returns a String describing this mailet.
     * 
     * @return A description of this mailet
     */
    public String getMailetInfo() {
        return "OverrideContentType";
    }

    /**
     * Checks every part in this part (if it is a Multipart) for having a
     * filename that matches the pattern. If the name matches, the content-type
     * value is overriden to user specified value.
     * 
     * Note: this method is recursive.
     * 
     * @param part
     *            The part to analyze.
     * @param mail
     * @return
     * @throws Exception
     */
    private boolean evalMessage(Part part, Mail mail)
            throws Exception {
        if (part.isMimeType("multipart/*")) {
            try {
            	Multipart multipart = (Multipart) part.getContent();
                int numParts = multipart.getCount();
                log ("multpartType part with "+numParts+" Contents");
                boolean changed = false;
                for (int i = 0; i < numParts; i++) {
                    BodyPart p = multipart.getBodyPart(i);
                    if (p.isMimeType("multipart/*")) {
                        changed |= evalMessage(p, mail);
                    } else {
                    	/* We have MIME message. Check the message for content-type */
                        String fileName = p.getFileName();
                        if (fileName != null) {
                        	/* We decode file and check content-type?? 
                        	 * Maybe later...
                        	 */
                            if (fileNameMatches(fileName)) {
                            	/* Matched file found */
                            	try {
                            		ContentType ct = new ContentType (p.getContentType());
                            		if (subTypePatternName==null || ct.getSubType().matches(subTypePatternName) == false) {
                                		log(p.getContentType());
                            			ct.setPrimaryType(PrimeTypeName);
                            			ct.setSubType(SubTypeName);
                            			p.setHeader("Content-Type", ct.toString());
                            			log(p.getContentType());
                            			changed = true;
                            		}
                            	} catch (Exception e) {
                            		log("Could not set Content-Type.", e);
                            	}
                            }
                        }
                    }
                }
                if (changed) {
                	part.setContent(multipart);
                	//((MimeMessage)mail.getMessage()).saveChanges();
                    if (part instanceof Message) {
                        ((Message) part).saveChanges();
                    }
                }
                return changed;
            } catch (Exception e) {
                log("Could not evaluate part.", e);
            }
        }
        return false;
    }

    /**
     * Checks if the given name matches the pattern.
     * 
     * @param name
     *            The name to check for a match.
     * @return True if a match is found, false otherwise.
     */
    private boolean fileNameMatches(String name) {
        boolean result = true;
        if (regExPattern != null)
            result = regExPattern.matcher(name).matches();

        String log = "attachment " + name + " ";
        if (!result)
            log += "does not match";
        else
            log += "matches";
        log(log);
        return result;
    }
}
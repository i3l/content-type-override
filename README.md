content-type-override
=====================

JAMES Email Server Mailet for Content-Type to Override Value

Some email client application does not set the content-type field in the email header. For Health IT system, CCD/CDA attachment should have content-type set to xml in the header to indicate that the attachment is XML document. Otherwise, the attachment will not be evaluated for CCD or CDA. This mailet checks the attachment's filename extension and override the value to specified value. 

For example, if you have XML attachments but client app entered as application/octet-stream, then the following mailet configuration can be used to change it to text/xml. This is required for the attached CCD file(s) to have text/xml or application/xml in order for MS HealthVault to start evaluating the attachment as a CCD file.

The mailet configuration shown below will scan all incoming messages and find an email that has an attachment with *.xml file extension. If found, unless the Content-Type's subType of attached file is marked as XML (subTypePattern), the Content Type's Primary and Sub type values will be overwritten with the information provided, text/xml in this example. Different user-defined file extension and content type values can be used in the mailet configuration. But, as this can cause an error at the receiver side if not properly overwritten, care must be taken with the mailet configurations.


```xml
<mailet match="All" class="org.gatech.i3l.mail.OverrideContentType">
  <filePattern >.*\.xml</filePattern>  <!-- RegExp that must be matched to filename -->
  <subTypePattern>[xX][mM][lL]</subTypePattern>  <!-- RegExp of SubType that will be ignored -->
  <primeType>text</primeType> <!-- Prime Type to be replaced with -->
  <subType>xml</subType> <!-- Sub Type to be replaced with -->
</mailet >
```

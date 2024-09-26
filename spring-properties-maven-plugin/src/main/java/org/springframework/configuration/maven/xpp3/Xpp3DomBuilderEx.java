package org.springframework.configuration.maven.xpp3;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;


public class Xpp3DomBuilderEx
{
    private static final boolean DEFAULT_TRIM = true;

    public static Xpp3DomEx build( Reader reader )
            throws XmlPullParserException, IOException
    {
        return build( reader, DEFAULT_TRIM );
    }


    public static Xpp3DomEx build( InputStream is, String encoding )
            throws XmlPullParserException, IOException
    {
        return build( is, encoding, DEFAULT_TRIM );
    }

    public static Xpp3DomEx build( InputStream is, String encoding, boolean trim )
            throws XmlPullParserException, IOException
    {
        XmlPullParser parser = new MXParser();

        parser.setInput( is, encoding );

        try
        {
            return build( parser, trim );
        }
        finally
        {
            close( is );
        }
    }

    public static Xpp3DomEx build( Reader reader, boolean trim )
            throws XmlPullParserException, IOException
    {
        XmlPullParser parser = new MXParser();

        parser.setInput( reader );

        try
        {
            return build( parser, trim );
        }
        finally
        {
            close( reader );
        }
    }

    public static Xpp3DomEx build( XmlPullParser parser )
            throws XmlPullParserException, IOException
    {
        return build( parser, DEFAULT_TRIM );
    }

    public static Xpp3DomEx build( XmlPullParser parser, boolean trim )
            throws XmlPullParserException, IOException
    {
        List<Xpp3DomEx> elements = new ArrayList<>();

        List<StringBuilder> values = new ArrayList<>();

        int eventType = parser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            switch (eventType) {
            case XmlPullParser.START_TAG: {
                String rawName = parser.getName();

                Xpp3DomEx childConfiguration = new Xpp3DomEx(rawName);

                int depth = elements.size();

                if (depth > 0) {
                    Xpp3DomEx parent = elements.get(depth - 1);

                    parent.addChild(childConfiguration);
                }

                elements.add(childConfiguration);

                if (parser.isEmptyElementTag()) {
                    values.add(null);
                } else {
                    values.add(new StringBuilder());
                }

                int attributesSize = parser.getAttributeCount();

                for (int i = 0; i < attributesSize; i++) {
                    String name = parser.getAttributeName(i);

                    String value = parser.getAttributeValue(i);

                    childConfiguration.setAttribute(name, value);
                }
                break;
            }
            case XmlPullParser.TEXT: {
                int depth = values.size() - 1;

                StringBuilder valueBuffer = values.get(depth);

                String text = parser.getText();

                if (trim) {
                    text = text.trim();
                }

                valueBuffer.append(text);
                break;
            }
            case XmlPullParser.END_TAG: {
                int depth = elements.size() - 1;

                Xpp3DomEx finishedConfiguration = elements.remove(depth);

                /* this Object could be null if it is a singleton tag */
                StringBuilder accumulatedValue = values.remove(depth);

                if (finishedConfiguration.getChildCount() == 0) {
                    if (accumulatedValue == null) {
                        finishedConfiguration.setValue(null);
                    } else {
                        finishedConfiguration.setValue(accumulatedValue.toString());
                    }
                }

                if (depth == 0) {
                    return finishedConfiguration;
                }
                break;
            }
            }

            eventType = parser.next();
        }

        throw new IllegalStateException( "End of document found before returning to 0 depth" );
    }

    public static Xpp3DomEx buildFirstTag( Reader reader )
            throws XmlPullParserException, IOException
    {
        return buildFirstTag( reader, DEFAULT_TRIM );
    }

    public static Xpp3DomEx buildFirstTag( Reader reader, boolean trim )
            throws XmlPullParserException, IOException
    {
        XmlPullParser parser = new MXParser();

        parser.setInput( reader );

        try
        {
            return buildFirstTag( parser, trim );
        }
        finally
        {
            close( reader );
        }
    }

    public static Xpp3DomEx buildFirstTag( XmlPullParser parser, boolean trim )
            throws XmlPullParserException, IOException
    {
        //List<Xpp3DomEx> elements = new ArrayList<>();

        Xpp3DomEx tag = null;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.START_TAG && eventType != XmlPullParser.END_DOCUMENT) {
            eventType = parser.next();
        }

        if (eventType == XmlPullParser.START_TAG) {
            String rawName = parser.getName();
            tag = new Xpp3DomEx(rawName);
            int attributesSize = parser.getAttributeCount();

            for (int i = 0; i < attributesSize; i++) {
                String name = parser.getAttributeName(i);

                String value = parser.getAttributeValue(i);

                tag.setAttribute(name, value);
            }
        }
        return tag;

    }

    private static void close( Closeable inputStream )
    {
        if ( inputStream == null )
        {
            return;
        }

        try
        {
            inputStream.close();
        }
        catch( IOException ex )
        {
            // ignore
        }
    }
}

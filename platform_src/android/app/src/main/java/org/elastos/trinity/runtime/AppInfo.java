 /*
  * Copyright (c) 2018 Elastos Foundation
  *
  * Permission is hereby granted, free of charge, to any person obtaining a copy
  * of this software and associated documentation files (the "Software"), to deal
  * in the Software without restriction, including without limitation the rights
  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  * copies of the Software, and to permit persons to whom the Software is
  * furnished to do so, subject to the following conditions:
  *
  * The above copyright notice and this permission notice shall be included in all
  * copies or substantial portions of the Software.
  *
  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  * SOFTWARE.
  */


 package org.elastos.trinity.runtime;

 import android.graphics.drawable.Icon;

 import java.util.ArrayList;

 public class AppInfo {

     public static final String TID = "tid";
     public static final String APP_TID = "app_tid";
     public static final String APP_ID = "app_id";
     public static final String VERSION = "version";
     public static final String NAME = "name";
     public static final String SHORT_NAME = "short_name";
     public static final String DESCRIPTION = "description";
     public static final String START_URL = "start_url";
     public static final String AUTHOR_NAME = "author_name";
     public static final String AUTHOR_EMAIL = "author_email";
     public static final String DEFAULT_LOCAL = "default_locale";
     public static final String BACKGROUND_COLOR = "background_color";
     public static final String THEME_DISPLAY = "theme_display";
     public static final String THEME_COLOR = "theme_color";
     public static final String THEME_FONT_NAME = "theme_font_name";
     public static final String THEME_FONT_COLOR = "theme_font_color";
     public static final String INSTALL_TIME = "install_time";
     public static final String BUILT_IN = "built_in";
     public static final String REMOTE = "remote";
     public static final String LAUNCHER = "launcher";

     public static final String SRC = "src";
     public static final String SIZES = "sizes";
     public static final String TYPE = "type";


     public static final String PLUGIN = "plugin";
     public static final String URL = "url";
     public static final String AUTHORITY = "authority";


     public static final int MSG_PARAMS = 0;
     public static final int MSG_RETURN = 1;
     public static final int MSG_URL_AUTHORITY = 2;
     public static final int MSG_PLUGIN_AUTHORITY = 3;

     public long tid;
     public String app_id;
     public String version;
     public String name;
     public String short_name;
     public String description;
     public String start_url;
     public String author_name;
     public String author_email;
     public String default_locale;
     public String background_color;
     public String theme_display;
     public String theme_color;
     public String theme_font_name;
     public String theme_font_color;
     public long   install_time;
     public int    built_in;
     public int    remote;
     public int    launcher;

     public static final int AUTHORITY_NOEXIST = -1;
     public static final int AUTHORITY_NOINIT = 0;
     public static final int AUTHORITY_ASK = 1;
     public static final int AUTHORITY_ALLOW = 2;
     public static final int AUTHORITY_DENY = 3;


     public class Icon {
         public String src = "";
         public String sizes = "";
         public String type = "";

         Icon(String src, String sizes, String type) {
             this.src = src;
             this.sizes = sizes;
             this.type = type;
         }
     }

     public class PluginAuth {
         public String plugin = "";
         public int authority = AUTHORITY_NOINIT;

         PluginAuth(String plugin, int authority) {
             this.plugin = plugin;
             this.authority = authority;
         }
     }

     public class UrlAuth {
         public String url;
         public int authority = AUTHORITY_NOINIT;

         UrlAuth(String url, int authority) {
             this.url = url;
             this.authority = authority;
         }
     }

     public ArrayList<Icon> icons = new ArrayList<Icon>(8);
     public ArrayList<PluginAuth> plugins = new ArrayList<PluginAuth>(20);
     public ArrayList<UrlAuth> urls = new ArrayList<UrlAuth>(20);

     public void addIcon(String src, String sizes, String type) {
         icons.add(new Icon(src, sizes, type));
     }

     public void addPlugin(String plugin, int authority) {
         plugins.add(new PluginAuth(plugin, authority));
     }

     public void addUrl(String url, int authority) {
         urls.add(new UrlAuth(url, authority));
     }
 }

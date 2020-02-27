package net.sourceforge.MSGViewer.MSGNavigator;

import at.redeye.FrameWork.base.AutoMBox;
import at.redeye.FrameWork.base.BaseDialog;
import at.redeye.FrameWork.base.Root;
import at.redeye.FrameWork.utilities.StringUtils;
import com.auxilii.msgparser.FieldInformation;
import com.auxilii.msgparser.PidTag;
import org.apache.poi.poifs.filesystem.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author martin
 */
public class MSGNavigator extends BaseDialog {

    public static final String SETTING_SHOW_SIZE = "MSG_NAVIGATOR_SHOW_SIZE";
    public static final String SETTING_AUTOSAVE = "MSG_SETTING_AUTOSAVE";

    private POIFSFileSystem fs;
    private final File file;

    private boolean setting_show_size = false;



     public static class TreeNodeContainer extends DefaultMutableTreeNode
     {
         private final Entry entry;
         private Object data;

         public TreeNodeContainer( Entry entry, String name  )
         {
             super( name );
             this.entry = entry;
         }

         public Entry getEntry() {
             return entry;
         }

         public Object getData() {
             return data;
         }

         public void setData( Object data ) {
             this.data = data;
         }

     }

     public static class PropertyContainer extends TreeNodeContainer
     {
         private final String tag;

         public PropertyContainer( Entry entry, String name, String property_tag )
         {
             super( entry, name );
             this.tag = property_tag;
         }

         public String getPropertyTag()
         {
             return tag;
         }
     }



    /** Creates new form MSGNavigator */
    public MSGNavigator(Root root, File file)
    {
        super( root, root.MlM("Navigate:") + " " + file.getName());

        initComponents();

        this.file = file;

        reload();
    }

    final void reload() {

        setting_show_size = StringUtils.isYes(root.getSetup().getLocalConfig(SETTING_SHOW_SIZE,"true") );

         EventQueue.invokeLater(() -> new AutoMBox(MSGNavigator.class.getName()) {
             @Override
             public void do_stuff() throws Exception {
                 parse(file);
             }
         });
    }

    void parse( File file ) throws IOException
    {
        InputStream in = new FileInputStream(file);

        fs = new POIFSFileSystem(in);
        DirectoryNode fs_root = fs.getRoot();

        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(fs_root.getShortDescription());

        parse( localRoot, fs_root );

        tree.setModel(new DefaultTreeModel(localRoot));
    }

    private void parse(DefaultMutableTreeNode root_node, DirectoryEntry fs_root) throws IOException
    {
        for (Entry entry : fs_root) {

            if (entry.isDirectoryEntry()) {
                DirectoryEntry de = (DirectoryEntry) entry;

                String name = "<html><body><b style=\"color: blue\">" +  entry.getName() + "</b></body></html>";

                DefaultMutableTreeNode node = new TreeNodeContainer(de,name);
                root_node.add(node);

                parse( node, de );
            } else if( entry.isDocumentEntry() ) {

                DocumentEntry de = (DocumentEntry) entry;

                FieldInformation info = analyzeDocumentEntry(de);

                // the data is accessed by getting an input stream
                // for the given document entry
                DocumentInputStream dstream = new DocumentInputStream(de);

                if( de.getName().equals("__properties_version1.0") )
                {
                    info.setType("0102");
                }

                // create a Java object from the data provided
                // by the input stream. depending on the field
                // information, either a String or a byte[] will
                // be returned. other datatypes are not yet supported
                Object data = this.getData(dstream, info);

                StringBuilder sb = new StringBuilder();

                sb.append("<html><body>");

                if( entry.getName().startsWith("__properties") ) {
                    sb.append("<b>");
                    sb.append(entry.getName());
                    sb.append("</b>");
                } else {
                    // sb.append("<b>");
                    sb.append(entry.getName());
                    // sb.append("</b>");
                }

                sb.append("&nbsp;&nbsp;&nbsp;");


                PidTag property_name =  PidTag.from(Integer.parseInt(info.getTag(), 16));

                sb.append(" <code style=\"color: green\">").append(property_name).append("</code> ");

                if( setting_show_size ) {
                    sb.append(" ").append(de.getSize()).append("b ");
                }

                if( data instanceof String )
                {
                    String s = (String)data;
                    if( s.length() > 100 ) {
                        s = s.substring(0,100) + "...";
                    }

                     sb.append("     <i>").append(s).append("</i>");
                } else if( data instanceof byte[] ) {
                    sb.append("     <i style=\"color: dd5555\">byte array</i>");
                }

                sb.append("</body></html>");

                TreeNodeContainer node = new TreeNodeContainer(entry,sb.toString());
                node.setData(data);

                if( de.getName().equals("__properties_version1.0") )
                {
                    try {
                        PropertyParser pp = new PropertyParser(de);
                        for (String tag : pp.getPropertyTags()) {
                            TreeNodeContainer pnode = new PropertyContainer(entry,
                                        "<html><body><pre style=\"font-family: monospace; fonz-size:8px\">"  + tag + "</pre></body></html>",
                                        tag);
                            node.add(pnode);
                        }
                    } catch (IOException ex) {
                        logger.error(ex, ex);
                    }
                }

                root_node.add(node);
            }
        }
    }

    /**
     * Analyzes the {@link DocumentEntry} and returns
     * a {@link FieldInformation} object containing the
     * class (the field name, so to say) and type of
     * the entry.
     *
     * @param de The {@link DocumentEntry} that should be examined.
     * @return A {@link FieldInformation} object containing class
     *  and type of the document entry or, if the entry is
     *  not an interesting field, an empty {@link FieldInformation}
     *  object containing {@link FieldInformation#UNKNOWN} class
     *  and type.
     */
    public static FieldInformation analyzeDocumentEntry(DocumentEntry de) {
        String name = de.getName();
        // we are only interested in document entries
        // with names starting with __substg1.0_
        String key = "__substg1.0_";
        if (name.startsWith(key)) {
            String val = name.substring(key.length()).toLowerCase();
            // the first 4 digits of the remainder
            // defines the field class (or field name)
            // and the last 4 digits indicate the
            // data type.
            String tag = val.substring(0, 4);
            String type = val.substring(4, 8);
            return new FieldInformation(tag, type);
        }
        // we are not interested in the field
        // and return an empty FieldInformation object
        return new FieldInformation();
    }

    /**
     * Reads the information from the InputStream and
     * creates, based on the information in the
     * {@link FieldInformation} object, either a String
     * or a byte[] (e.g., for attachments) Object
     * containing this data.
     *
     * @param dstream The InputStream of the Document Entry.
     * @param info The field information that is needed to
     *  determine the data type of the input stream.
     * @return The String/byte[] object representing
     *  the data.
     * @throws IOException Thrown if the .msg file could not
     *  be parsed.
     * @throws UnsupportedOperationException Thrown if
     *  the .msg file contains unknown data.
     */
    private Object getData(DocumentInputStream dstream, FieldInformation info) throws IOException {
        // if there is no field information available, we simply
        // return null. in that case, we're not interested in the
        // data anyway
        if (info == null || FieldInformation.UNKNOWN.equals(info.getType())) {
            return null;
        }
        // if the type is 001e (we know it is lower case
        // because analyzeDocumentEntry stores the type in
        // lower case), we create a String object from the data.
        // the encoding of the binary data is most probably
        // ISO-8859-1 (not pure ASCII).
        switch (info.getType()) {
            case "001e": return new String(read(dstream), StandardCharsets.ISO_8859_1);
            case "001f": return new String(read(dstream), StandardCharsets.UTF_16LE);
            case "0102": return read(dstream);
            case "0000":
            {
                // the data is read into a byte[] object
                byte[] buffer = new byte[1024];

                StringBuilder sb = new StringBuilder();

                for (int read; (read = dstream.read(buffer)) > 0;) {
                    for (int i = 0; i < read; i++) {
                        sb.append(buffer[i]);
                        sb.append(" ");
                    }
                }
                return sb.toString();
            }
        }

        // this should not happen
        logger.trace("Unknown field type " + info.getType());
        return null;
    }

	private byte[] read(InputStream stream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		for (int read; (read = stream.read(buffer)) > 0;) {
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(250, 250));

        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                treeMouseClicked(evt);
            }
        });
        scrollPane.setViewportView(tree);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 611, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 463, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void treeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_treeMouseClicked

        if( evt.getButton() == MouseEvent.BUTTON3 )
        {

            TreePath path = tree.getPathForLocation(evt.getX(), evt.getY());

            if( path == null ) {
                // retry it with lower X value
                for( int x = evt.getX(); x > 0 && path == null; x -= 10 ) {
                    path = tree.getPathForLocation(x, evt.getY());
                }
            }

            if (path != null) {
                tree.setSelectionPath(path);
            }


            JPopupMenu popup = new NavActionPopup(this);

            popup.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_treeMouseClicked

    public void deleteSelectedElement()
    {
        new AutoMBox(this.getClass().getName()) {

            @Override
            public void do_stuff() throws Exception {
                TreePath path = tree.getSelectionPath();

                if (path == null) {
                    return;
                }

                TreeNodeContainer cont = (TreeNodeContainer) path.getLastPathComponent();

                logger.info("deleting : " + cont.getEntry().getName());

                TopLevelPropertyStream tops = new TopLevelPropertyStream(fs.getRoot());
                tops.delete(cont.getEntry());

                cont.removeFromParent();
                tree.updateUI();

                if( StringUtils.isYes(root.getSetup().getLocalConfig(SETTING_AUTOSAVE,"false")) )
                {
                    save();
                    reload();
                }
            }
        };
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTree tree;
    // End of variables declaration//GEN-END:variables

    void save() throws IOException
    {
       if( fs == null ) {
           return;
       }

        try (OutputStream out = new FileOutputStream(file)) {
            fs.writeFilesystem(out);
        }
    }

    @Override
    public void close()
    {
        root.getSetup().setLocalConfig(SETTING_SHOW_SIZE, String.valueOf(setting_show_size));
        super.close();
    }

    void showSelectedElement()
    {
        TreePath path = tree.getSelectionPath();

        if( path == null ) {
            return;
        }

        TreeNodeContainer cont = (TreeNodeContainer) path.getLastPathComponent();

        invokeDialog(new ShowNode(root, cont));
    }


    void editSelectedElement() {
        TreePath path = tree.getSelectionPath();

        if( path == null ) {
            return;
        }

        TreeNodeContainer cont = (TreeNodeContainer) path.getLastPathComponent();

        invokeDialog(new EditNode(root, this, cont));
    }

    void edited() throws IOException {

        if (StringUtils.isYes(root.getSetup().getLocalConfig(SETTING_AUTOSAVE, "false"))) {
            save();
            reload();
        }
    }

}

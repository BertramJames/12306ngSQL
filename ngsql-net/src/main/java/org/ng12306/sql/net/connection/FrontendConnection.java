package org.ng12306.sql.net.connection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ng12306.ngsql.model.Capabilities;
import org.ng12306.ngsql.model.Versions;
import org.ng12306.ngsql.util.ErrorCode;
import org.ng12306.ngsql.util.NameableExecutor;
import org.ng12306.ngsql.util.RandomUtil;
import org.ng12306.ngsql.util.TimeUtil;
import org.ng12306.sql.net.AbstractConnection;
import org.ng12306.sql.net.Handler;
import org.ng12306.sql.net.Processor;
import org.ng12306.sql.net.handler.Authenticator;
import org.ng12306.sql.net.handler.PrepareHandler;
import org.ng12306.sql.net.handler.Privileges;
import org.ng12306.sql.net.handler.QueryHandler;
import org.ng12306.sql.net.mysql.CharsetUtil;
import org.ng12306.sql.net.mysql.ErrorPacket;
import org.ng12306.sql.net.mysql.HandshakePacket;
import org.ng12306.sql.net.mysql.MySQLMessage;
import org.ng12306.sql.net.mysql.OkPacket;


public abstract class FrontendConnection extends AbstractConnection {

	private static final Logger LOGGER = Logger.getLogger(FrontendConnection.class);

    protected long id;
    protected String host;
    protected int port;
    protected int localPort;
    protected long idleTimeout;
    protected String charset;
    protected int charsetIndex;
    protected byte[] seed;
    protected String user;
    protected String schema;
    protected Handler handler;
    protected Privileges privileges;
    protected QueryHandler queryHandler;
    protected PrepareHandler prepareHandler;
    protected boolean isAccepted;
    protected boolean isAuthenticated;

    public FrontendConnection(SocketChannel channel) {
        super(channel);
        Socket socket = channel.socket();
        this.host = socket.getInetAddress().getHostAddress();
        this.port = socket.getPort();
        this.localPort = socket.getLocalPort();
        this.handler = new Authenticator(this);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public long getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(long idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public boolean isIdleTimeout() {
        return TimeUtil.currentTimeMillis() > Math.max(lastWriteTime, lastReadTime) + idleTimeout;
    }

    public void setAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
        this.readBuffer = processor.getBufferPool().allocate();
        processor.addFrontend(this);
    }

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    public void setQueryHandler(QueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }

    public void setPrepareHandler(PrepareHandler prepareHandler) {
        this.prepareHandler = prepareHandler;
    }

    public void setAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
    }

    public Privileges getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Privileges privileges) {
        this.privileges = privileges;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public byte[] getSeed() {
        return seed;
    }

    public int getCharsetIndex() {
        return charsetIndex;
    }

    public boolean setCharsetIndex(int ci) {
        String charset = CharsetUtil.getCharset(ci);
        if (charset != null) {
            this.charset = charset;
            this.charsetIndex = ci;
            return true;
        } else {
            return false;
        }
    }

    public String getCharset() {
        return charset;
    }

    public boolean setCharset(String charset) {
        int ci = CharsetUtil.getIndex(charset);
        if (ci > 0) {
            this.charset = charset;
            this.charsetIndex = ci;
            return true;
        } else {
            return false;
        }
    }

    public void writeErrMessage(int errno, String msg) {
        writeErrMessage((byte) 1, errno, msg);
    }

    public void writeErrMessage(byte id, int errno, String msg) {
        ErrorPacket err = new ErrorPacket();
        err.packetId = id;
        err.errno = errno;
        err.message = encodeString(msg, charset);
        err.write(this);
    }

    public void initDB(byte[] data) {
        MySQLMessage mm = new MySQLMessage(data);
        mm.position(5);
        String db = mm.readString();

        // 检查schema是否已经设置
        if (schema != null) {
            if (schema.equals(db)) {
                write(writeToBuffer(OkPacket.OK, allocate()));
            } else {
                writeErrMessage(ErrorCode.ER_DBACCESS_DENIED_ERROR, "Not allowed to change the database!");
            }
            return;
        }

        // 检查schema的有效性
        if (db == null || !privileges.schemaExists(db)) {
            writeErrMessage(ErrorCode.ER_BAD_DB_ERROR, "Unknown database '" + db + "'");
            return;
        }
        if (!privileges.userExists(user, host)) {
            writeErrMessage(ErrorCode.ER_ACCESS_DENIED_ERROR, "Access denied for user '" + user + "'");
            return;
        }
        Set<String> schemas = privileges.getUserSchemas(user);
        if (schemas == null || schemas.size() == 0 || schemas.contains(db)) {
            this.schema = db;
            write(writeToBuffer(OkPacket.OK, allocate()));
        } else {
            String s = "Access denied for user '" + user + "' to database '" + db + "'";
            writeErrMessage(ErrorCode.ER_DBACCESS_DENIED_ERROR, s);
        }
    }

    public void query(byte[] data) {
        if (queryHandler != null) {
            // 取得语句
            MySQLMessage mm = new MySQLMessage(data);
            mm.position(5);
            String sql = null;
            try {
                sql = mm.readString(charset);
            } catch (UnsupportedEncodingException e) {
                writeErrMessage(ErrorCode.ER_UNKNOWN_CHARACTER_SET, "Unknown charset '" + charset + "'");
                return;
            }
            if (sql == null || sql.length() == 0) {
                writeErrMessage(ErrorCode.ER_NOT_ALLOWED_COMMAND, "Empty SQL");
                return;
            }

            // 执行查询
            queryHandler.query(sql);
        } else {
            writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Query unsupported!");
        }
    }

    public void stmtPrepare(byte[] data) {
        if (prepareHandler != null) {
            // 取得语句
            MySQLMessage mm = new MySQLMessage(data);
            mm.position(5);
            String sql = null;
            try {
                sql = mm.readString(charset);
            } catch (UnsupportedEncodingException e) {
                writeErrMessage(ErrorCode.ER_UNKNOWN_CHARACTER_SET, "Unknown charset '" + charset + "'");
                return;
            }
            if (sql == null || sql.length() == 0) {
                writeErrMessage(ErrorCode.ER_NOT_ALLOWED_COMMAND, "Empty SQL");
                return;
            }

            // 执行预处理
            prepareHandler.prepare(sql);
        } else {
            writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Prepare unsupported!");
        }
    }

    public void stmtExecute(byte[] data) {
        if (prepareHandler != null) {
            prepareHandler.execute(data);
        } else {
            writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Prepare unsupported!");
        }
    }

    public void stmtClose(byte[] data) {
        if (prepareHandler != null) {
            prepareHandler.close();
        } else {
            writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Prepare unsupported!");
        }
    }

    public void ping() {
        write(writeToBuffer(OkPacket.OK, allocate()));
    }

    public void heartbeat(byte[] data) {
        write(writeToBuffer(OkPacket.OK, allocate()));
    }

    public void kill(byte[] data) {
        writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Unknown command");
    }

    public void unknown(byte[] data) {
        writeErrMessage(ErrorCode.ER_UNKNOWN_COM_ERROR, "Unknown command");
    }

    @Override
    protected void idleCheck() {
        if (isIdleTimeout()) {
            LOGGER.warn(toString() + " idle timeout");
            close();
        }
    }

    @Override
    public void register(Selector selector) throws IOException {
        super.register(selector);
        if (!isClosed.get()) {
            // 生成认证数据
            byte[] rand1 = RandomUtil.randomBytes(8);
            byte[] rand2 = RandomUtil.randomBytes(12);

            // 保存认证数据
            byte[] seed = new byte[rand1.length + rand2.length];
            System.arraycopy(rand1, 0, seed, 0, rand1.length);
            System.arraycopy(rand2, 0, seed, rand1.length, rand2.length);
            this.seed = seed;

            // 发送握手数据包
            HandshakePacket hs = new HandshakePacket();
            hs.packetId = 0;
            hs.protocolVersion = Versions.PROTOCOL_VERSION;
            hs.serverVersion = Versions.SERVER_VERSION;
            hs.threadId = id;
            hs.seed = rand1;
            hs.serverCapabilities = getServerCapabilities();
            hs.serverCharsetIndex = (byte) (charsetIndex & 0xff);
            hs.serverStatus = 2;
            hs.restOfScrambleBuff = rand2;
            hs.write(this);
        }
    }

    @Override
    public void handle(final byte[] data) {
        // 异步处理前端数据
        processor.getHandler().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.handle(data);
                } catch (Throwable t) {
                    error(ErrorCode.ERR_HANDLE_DATA, t);
                }
            }
        });
    }

    protected int getServerCapabilities() {
        int flag = 0;
        flag |= Capabilities.CLIENT_LONG_PASSWORD;
        flag |= Capabilities.CLIENT_FOUND_ROWS;
        flag |= Capabilities.CLIENT_LONG_FLAG;
        flag |= Capabilities.CLIENT_CONNECT_WITH_DB;
        // flag |= Capabilities.CLIENT_NO_SCHEMA;
        // flag |= Capabilities.CLIENT_COMPRESS;
        flag |= Capabilities.CLIENT_ODBC;
        // flag |= Capabilities.CLIENT_LOCAL_FILES;
        flag |= Capabilities.CLIENT_IGNORE_SPACE;
        flag |= Capabilities.CLIENT_PROTOCOL_41;
        flag |= Capabilities.CLIENT_INTERACTIVE;
        // flag |= Capabilities.CLIENT_SSL;
        flag |= Capabilities.CLIENT_IGNORE_SIGPIPE;
        flag |= Capabilities.CLIENT_TRANSACTIONS;
        // flag |= ServerDefs.CLIENT_RESERVED;
        flag |= Capabilities.CLIENT_SECURE_CONNECTION;
        return flag;
    }

    protected boolean isConnectionReset(Throwable t) {
        if (t instanceof IOException) {
            String msg = t.getMessage();
            return (msg != null && msg.contains("Connection reset by peer"));
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("[thread=").append(Thread.currentThread().getName()).append(",class=")
                .append(getClass().getSimpleName()).append(",host=").append(host).append(",port=").append(port)
                .append(",schema=").append(schema).append(']').toString();
    }

    private final static byte[] encodeString(String src, String charset) {
        if (src == null) {
            return null;
        }
        if (charset == null) {
            return src.getBytes();
        }
        try {
            return src.getBytes(charset);
        } catch (UnsupportedEncodingException e) {
            return src.getBytes();
        }
    }
    
    
}
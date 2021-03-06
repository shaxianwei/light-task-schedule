package com.lts.job.core.registry;

import com.lts.job.core.cluster.Node;
import com.lts.job.core.cluster.NodeType;
import com.lts.job.core.util.NetUtils;
import com.lts.job.core.util.StringUtils;

import java.util.Date;

/**
 * @author Robert HG (254963746@qq.com) on 5/11/15.
 *         <p/>
 *         /LTS/{集群名字}/NODES/TASK_TRACKER/TASK_TRACKER:\\192.168.0.150:8888?group=TASK_TRACKER&threads=8&identity=85750db6-e854-4eb3-a595-9227a5f2c8f6&createTime=1408189898185&isAvailable=true&listenNodeTypes=CLIENT,TASK_TRACKER
 *         /LTS/{集群名字}/NODES/JOB_CLIENT/JOB_CLIENT:\\192.168.0.150:8888?group=JOB_CLIENT&threads=8&identity=85750db6-e854-4eb3-a595-9227a5f2c8f6&createTime=1408189898185&isAvailable=true&listenNodeTypes=CLIENT,TASK_TRACKER
 *         /LTS/{集群名字}/NODES/JOB_TRACKER/JOB_TRACKER:\\192.168.0.150:8888?group=JOB_TRACKER&threads=8&identity=85750db6-e854-4eb3-a595-9227a5f2c8f6&createTime=1408189898185&isAvailable=true&listenNodeTypes=CLIENT,TASK_TRACKER
 *         <p/>
 */
public class NodeRegistryUtils {

    public static String getRootPath(String clusterName) {
        return "/LTS/" + clusterName + "/NODES";
    }

    public static String getNodeTypePath(String clusterName, NodeType nodeType) {
        return NodeRegistryUtils.getRootPath(clusterName) + "/" + nodeType;
    }

    public static Node parse(String fullPath) {
        Node node = new Node();
        String[] nodeDir = fullPath.split("/");
        NodeType nodeType = NodeType.valueOf(nodeDir[4]);
        node.setNodeType(nodeType);
        String url = nodeDir[5];

        url = url.substring(nodeType.name().length() + 3);
        String address = url.split("\\?")[0];
        String ip = address.split(":")[0];

        node.setIp(ip);
        if (address.contains(":")) {
            String port = address.split(":")[1];
            if (port != null && !"".equals(port.trim())) {
                node.setPort(Integer.valueOf(port));
            }
        }
        String params = url.split("\\?")[1];

        String[] paramArr = params.split("&");
        for (String paramEntry : paramArr) {
            String key = paramEntry.split("=")[0];
            String value = paramEntry.split("=")[1];

            if ("group".equals(key)) {
                node.setGroup(value);
            } else if ("threads".equals(key)) {
                node.setThreads(Integer.valueOf(value));
            } else if ("identity".equals(key)) {
                node.setIdentity(value);
            } else if ("createTime".equals(key)) {
                node.setCreateTime(Long.valueOf(value));
            } else if ("isAvailable".equals(key)) {
                node.setAvailable(Boolean.valueOf(value));
            }
        }
        return node;
    }

    public static String getFullPath(Node node) {
        StringBuilder path = new StringBuilder();

        path.append(getRootPath(node.getClusterName()))
                .append("/")
                .append(node.getNodeType())
                .append("/")
                .append(node.getNodeType())
                .append(":\\\\")
                .append(node.getIp());

        if (node.getPort() != null && node.getPort() != 0) {
            path.append(":").append(node.getPort());
        }

        path.append("?")
                .append("group=")
                .append(node.getGroup());
        if (node.getThreads() != 0) {
            path.append("&threads=")
                    .append(node.getThreads());
        }

        path.append("&identity=")
                .append(node.getIdentity())
                .append("&createTime=")
                .append(node.getCreateTime())
                .append("&isAvailable=")
                .append(node.isAvailable());

        return path.toString();
    }

    public static void main(String[] args) {
        Node node = new Node();
        node.setGroup("group1");
        node.setIdentity(StringUtils.generateUUID());
        node.setThreads(222);
        node.setNodeType(NodeType.JOB_TRACKER);
        node.setCreateTime(new Date().getTime());
        node.setPort(2313);
        node.setClusterName("lts");
        node.setIp(NetUtils.getLocalHost());
        String fullPath = NodeRegistryUtils.getFullPath(node);
        System.out.println(fullPath);

        node = NodeRegistryUtils.parse(fullPath);
        node.setNodeType(NodeType.JOB_CLIENT);
        fullPath = NodeRegistryUtils.getFullPath(node);
        System.out.println(fullPath);

        node = NodeRegistryUtils.parse(fullPath);
        System.out.println(node);
    }
}

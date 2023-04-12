"""Zeekflow Integration API
An API written in Python, using Flask module to create a simple API for some operations. This API will receive .csv files that contain
netflows and will integrate them with the zeekflow connection log, adding zeek score in csv, and send it back to client.

Execution Params:
- Flask Server Params:
param --port -p: Port that API will listens to, defaults to 7100 (optional).

param --directory -d: Directory where zeeklogs will be stored to, defaults to /home/zeekflow-integration/logs/ (optional).
param --logfile -l: Name of zeeklog file, defaults to conn.log (optional).

param --benchmark -b: Enabled benchmark mode, defaults to False(optional).
"""

import os
import sys
import logging
import argparse
import base64

import pandas as pd

from flask import Flask, request

# Initialize logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s %(levelname)s %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)

# Initialize Flask app
app = Flask("Palantir CSV Source Connector")


def parseArguments():
    parser = argparse.ArgumentParser(
        description='Zeekflow Integration API Params.')

    # Arguments for Flask server
    apiArgsGroup = parser.add_argument_group('Zeekflow API Params')
    apiArgsGroup.add_argument('-p', '--port', type=int, required=False, default=7100,
        help='Port that API will listens to. Default: 7100')
    
    # Zeek logs configuration
    zeekArgsGroup = parser.add_argument_group('Zeek Logs Params')
    zeekArgsGroup.add_argument('-d', '--directory', type=str, required=False, default='/home/zeekflow-integration/logs/',
        help='Directory, where zeek logs will be stored to. Default: /home/zeekflow-integration/logs/')
    zeekArgsGroup.add_argument('-l', '--logfile', type=str, required=False, default='conn.log',
        help='File, where zeek logs are synced. Default: conn.log')

    # Other configuration
    otherArgsGroup = parser.add_argument_group('Other Params')
    otherArgsGroup.add_argument('-b', '--benchmark', type=bool, required=False, default=False,
        help='Run Zeekflow API in benchmark mode. Default: False')

    args = parser.parse_args()

    return args

# Zeekflow integration API. Gets a netflow file and zeek log and integrates them.
def ZeekFlow(netflowPath, zeekPath):
    header_cols = [
        'ts', 'te', 'td', 'sa', 'da', 'sp', 'dp', 'pr', 'flg', 'fwd', 'stos',
        'ipkt', 'ibyt', 'opkt', 'obyt', 'in', 'out', 'sas', 'das', 'smk', 'dmk',
        'dtos', 'dir', 'nh', 'nhb', 'svln', 'dvln', 'ismc', 'odmc', 'idmc',
        'osmc', 'mpls1', 'mpls2', 'mpls3', 'mpls4', 'mpls5', 'mpls6', 'mpls7',
        'mpls8', 'mpls9', 'mpls10', 'cl', 'sl', 'al', 'ra', 'eng', 'exid', 'tr'
    ]
    output_cols = [
        'ts', 'te', 'td', 'sa', 'da', 'sp', 'dp', 'pr', 'flg', 'fwd', 'stos',
        'ipkt', 'ibyt', 'opkt', 'obyt', 'in', 'out', 'sas', 'das', 'smk', 'dmk',
        'dtos', 'dir', 'nh', 'nhb', 'svln', 'dvln', 'ismc', 'odmc', 'idmc',
        'osmc', 'mpls1', 'mpls2', 'mpls3', 'mpls4', 'mpls5', 'mpls6', 'mpls7',
        'mpls8', 'mpls9', 'mpls10', 'cl', 'sl', 'al', 'ra', 'eng', 'exid', 'tr','history'
    ]
    
    netflow = pd.read_csv(netflowPath, header=None, names=header_cols, index_col=False)

    zeek_f = open(zeekPath, 'r')
    zeek_lines = zeek_f.readlines()
    zeek = []
    conn_order = ["ts", "uid", "id.orig_h", "id.orig_p", "id.resp_h", "id.resp_p", "proto", "service", "duration", "orig_bytes", "resp_bytes",
        "conn_state", "local_orig", "local_resp", "missed_bytes", "history", "orig_pkts", "orig_ip_bytes", "resp_pkts", "resp_ip_bytes", "tunnel_parents"]
    for line in zeek_lines[8:-1]:
        details = line.split('	')
        details = [x.strip() for x in details]
        structure = {key: value for key, value in zip(conn_order, details)}
        zeek.append(structure)
    zeek = pd.DataFrame(zeek, columns=zeek[0].keys())
    zeek.duration = pd.to_numeric(zeek.duration, errors='coerce')
    zeek = zeek.dropna()

    netflow = netflow.dropna()
    netflow.dp = netflow.dp.astype(int)
    netflow.sa = netflow.sa.astype(str)
    netflow.da = netflow.da.astype(str)
    netflow.sp = netflow.sp.astype(int)
    netflow.dp = netflow.dp.astype(int)
    netflow.dp = netflow.dp.astype(int)
    netflow.ipkt = netflow.ipkt.astype(int)
    netflow.opkt = netflow.opkt.astype(int)

    zeek['id.orig_h'] = zeek['id.orig_h'].astype(str)
    zeek['id.resp_h'] = zeek['id.resp_h'].astype(str)
    zeek['id.orig_p'] = zeek['id.orig_p'].astype(int)
    zeek['id.resp_p'] = zeek['id.resp_p'].astype(int)
    zeek['orig_pkts'] = zeek['orig_pkts'].astype(int)
    zeek['resp_pkts'] = zeek['resp_pkts'].astype(int)
    netflow.sp = netflow.sp.astype(int)
    netflow.dp = netflow.dp.astype(int)
    netflow_to_zeek = {"sa": "id.orig_h", "da": "id.resp_h",
        "sp": "id.orig_p", "dp": "id.resp_p"}
    netflow = netflow.rename(columns=netflow_to_zeek)
    common_cols = ['id.orig_h', 'id.resp_h', 'id.orig_p',
        'id.resp_p']  # ,'orig_pkts','resp_pkts']
    zeek = zeek.drop_duplicates(subset=common_cols, keep='last')
    # extract common rows with merge

    print('[DEBUG] Netflow DF count before: ' + str(netflow.shape))
    print('[DEBUG] Zeek DF count before: ' + str(zeek.shape))

    df12 = pd.merge(netflow, zeek, on=common_cols, how='left')

    print('[DEBUG] DF-12 count after: ' + str(df12.shape))

    zeek_to_netflow = {v: k for k, v in netflow_to_zeek.items()}
    df12 = df12.rename(columns=zeek_to_netflow)
    df12 = df12.drop(['ts_y'], axis=1)

    df = df12.rename(columns={'ts_x': 'ts'})

    df['history'] = df['history'].fillna("$")

    return df[output_cols]

# Retrieve a netflow .csv file and integrate it with zeekflow, adding zeek score in the returned .csv file.
@app.route("/zeekflow", methods=["POST"])
def integrateZeek():
    """(POST) Retrieve netflow file route.
    A route used for retrieving sent netflow files, in .csv format. When a file is received it is 
    integrated with zeekflowappending the zeekscore and sent it back to client.

    Parameters
    ----------
    param filename: Filename of .csv file to be collected. Only filename, no path

    Request Body
    ------------
    File Content (in binary mode)

    Responses (response, status code)
    ---------------------------------
    {"result": "success", "zeek": "BASE64 encoded .csv netflow file with zeek score"}, 200
    {"result": "error", "message": "A message describing the error occured"}, 400
    """

    response = {}
    statusCode = 200
    
    try:
        csv_filename = '/home/zeekflow-integration/collected_files_csv/' + str(request.args['filename']) + '.csv'
        zeek_filename = '/home/zeekflow-integration/collected_files_zeek/' + str(request.args['filename']) + '.csv'

        f = open(csv_filename, 'wb')
        f.write(request.data)
        f.close()

        # Integrate with Zeek. Add a history column.
        zeeklog = args.directory + '/' + args.logfile
        zeek_df = ZeekFlow(csv_filename, zeeklog)
        # Store them to a .csv file
        zeek_df.to_csv(zeek_filename, header=None, index=False)

        # Convert .csv file with zeek score to Base64 format and return
        with open(zeek_filename, "rb") as zeek_netflow_file:
            zeek_base64 = base64.b64encode(zeek_netflow_file.read())

        # Delete the posted .csv and the zeek created .csv_filename
        if(os.path.exists(csv_filename)):
            os.remove(csv_filename)
        if(os.path.exists(zeek_filename)):
            os.remove(zeek_filename)


        statusCode = 200
        response['result'] = 'success'
        response['zeek'] = zeek_base64.decode("utf-8")
    except Exception as e:
        logger.exception(e)

        statusCode = 400
        response['result'] = 'error'
        response['message'] = str(e)

    return response, statusCode

if __name__ == "__main__":
    # Parse command line arguments
    args = parseArguments()

    # Start Flask Server
    app.run(
        host='0.0.0.0',
        port=args.port,
        debug=False
    )
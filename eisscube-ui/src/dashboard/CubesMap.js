import React, { Component } from 'react';
import _ from 'lodash';
import L from 'leaflet';
import { Map, LayersControl, TileLayer, Marker, Popup } from 'react-leaflet';
import { GET_LIST } from 'react-admin';
import DataProvider from '../rest/DataProvider';
import { red, green } from '@material-ui/core/colors';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.js';

import MarkerClusterGroup from 'react-leaflet-markercluster';

const { BaseLayer } = LayersControl;

const redMarker = L.AwesomeMarkers.icon({
    icon: 'circle',
    prefix: 'fa',
    markerColor: 'red'
});

const greenMarker = L.AwesomeMarkers.icon({
    icon: 'circle',
    prefix: 'fa',
    markerColor: 'green'
});

class CubesMap extends Component {
    constructor(props) {
        super(props);
        this.state = {
            lat: 40.2769179,
            lng: -74.0388226,
            zoom: 10,
            maxZoom: 18,
            data: null
        };
    }

    componentWillMount() {
        DataProvider(GET_LIST, 'cubes', {
            sort: { field: 'deviceID', order: 'ASC' },
            pagination: { page: 1, perPage: 100 }
        })
        .then(response => response.data)
        .then(data => {
            if (data) {
                this.setState({ data });
            }
        });
    }

    render() {
        const position = [this.state.lat, this.state.lng]
        const { data } = this.state;

        const markers = _.map(data, (cube) => {
            const lat = cube.location ? cube.location.lat : this.state.lat;
            const lng = cube.location ? cube.location.lng : this.state.lng;
            const status = cube.online 
                ? <div style={{color: green[500], textAlign: 'center'}}>ONLINE</div> 
                : <div style={{color: red[500], textAlign: 'center'}}>OFFLINE</div>;
            const icon = cube.online ? greenMarker : redMarker;
            return (
                <Marker 
                    position={[lat, lng]}
                    icon={icon}
                >
                    <Popup minWidth={200}>
                        {status}
                        <span>
                            Name: <b>{cube.deviceID}</b>
                            <br/>
                            Customer: {cube.customerID}
                            <br/>
                            <a href={`#/commands?filter={"q":"${cube.id}"}&page=1&perPage=10&sort=created&order=DESC`}>Commands</a>
                            <br/>
                            <a href={`#/reports?filter={"q":"${cube.id}"}&page=1&perPage=10&sort=reportID&order=ASC`}>Reports</a>
                        </span>
                    </Popup>
                </Marker>               
            );
        });

        return (
            <Map center={position} zoom={this.state.zoom} maxZoom={this.state.maxZoom}>
                <LayersControl position="topright">
                    <BaseLayer checked name="Color">
                        <TileLayer
                            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
                            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                    </BaseLayer>
                    <BaseLayer name="Black And White">
                        <TileLayer
                            attribution="&amp;copy <a href=&quot;http://osm.org/copyright&quot;>OpenStreetMap</a> contributors"
                            url="http://{s}.tiles.wmflabs.org/bw-mapnik/{z}/{x}/{y}.png"
                        />
                    </BaseLayer>
                    <MarkerClusterGroup>
                        {markers}
                    </MarkerClusterGroup>
                </LayersControl>
             </Map>
        );
    }
}

export default CubesMap;

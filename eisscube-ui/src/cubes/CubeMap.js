import React, { Component } from 'react';
import { connect } from 'react-redux';
import _ from 'lodash';
import L from 'leaflet';
import { Map, LayersControl, TileLayer, Marker, Popup } from 'react-leaflet';
//import { Map, TileLayer, Marker, Popup, FeatureGroup, LayerGroup, Circle, Polygon, Polyline, GeoJSON } from 'react-leaflet';
//import { EditControl } from 'react-leaflet-draw';

import PropTypes from 'prop-types';
import { showNotification, GET_ONE, CREATE, UPDATE } from 'react-admin';
import DataProvider from '../rest/DataProvider';

import { red500, green500 } from '@material-ui/core/colors';

import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.css';
import 'leaflet.awesome-markers/dist/leaflet.awesome-markers.js';

const { BaseLayer } = LayersControl;

const blueMarker = L.AwesomeMarkers.icon({
  icon: 'circle',
  prefix: 'fa',
  markerColor: 'blue'
});

class CubeMap extends Component {
  constructor(props) {
    super(props);
    const { location } = this.props.record;
    this.state = {
      marker: {
        lat: location ? location.lat : 40.2769179,
        lng: location ? location.lng : -74.0388226,
      },
      draggable: true,
      //geo: null,
      //layers: []
    };

    //this.onMounted = this.onMounted.bind(this);
    //this.onCreated = this.onCreated.bind(this);
    //this.onEdited = this.onEdited.bind(this);
    //this.onDeleted = this.onDeleted.bind(this);
  }

  // onMounted(drawControl) {
  //   restClient(GET_ONE, 'geometry', {
  //     id: this.props.record.deviceID
  //   })
  //   .then(response => response.data)
  //   .then(data => {
  //     if (data) {
  //       this.setState({ geo: data.geo });
  //
  //       // const geojsonLayer = L.geoJson(data.geo, {
  //       //   onEachFeature: this.onEachFeature,
  //       //   pointToLayer: this.pointToLayer,
  //       //   filter: this.filterFeatures
  //       // });
  //       // geojsonLayer.addTo(this.refs.map.leafletElement);
  //
  //     }
  //   });
  // }

  // onEachFeature(feature, layer) {
  //   // var layers = this.state.layers;
  //   // layers.push(layer);
  //   // this.setState({ layers: layers});
  //
  //   if (feature.properties && feature.properties.NAME && feature.properties.LINE) {
  //
  //     const popupContent = `<h3>${feature.properties.NAME}</h3>
  //       <strong>Access to MTA lines: </strong>${feature.properties.LINE}`;
  //
  //     // add our popups
  //     layer.bindPopup(popupContent);
  //   }
  // }
  //
  // pointToLayer(feature, latlng) {
  //   var markerParams = {
  //     radius: 10,
  //     fillColor: 'orange',
  //     color: '#fff',
  //     weight: 1,
  //     opacity: 0.5,
  //     fillOpacity: 0.8
  //   };
  //
  //   return L.circleMarker(latlng, markerParams);
  // }
  //
  // filterFeatures(feature, layer) {
  //   return true; // show feature
  // }

  // onCreated(e) {
  //   console.log('Created !');
  //   var geo = this.state.geo;
  //   var features = geo.features;
  //   features.push(e.layer.toGeoJSON());
  //
  //   this.setState({ geo: geo })
  // }

  // onEdited(e) {
  //   var layers = e.layers;
  //
  //   var countOfEditedLayers = 0;
  //   layers.eachLayer(function (layer) {
  //     countOfEditedLayers++;
  //   });
  //   console.log("Edited " + countOfEditedLayers + " layers");
  //
  //   if (countOfEditedLayers > 0) {
  //     restClient(CREATE, 'geometry', {
  //       data: {
  //         deviceID: this.props.record.deviceID,
  //         geo: layers.toGeoJSON()
  //       }
  //     })
  //     .then(() => {
  //       showNotification('Location approved');
  //     })
  //     .catch((e) => {
  //       console.error(e);
  //       showNotification('Error: comment not approved', 'warning')
  //     });
  //   }
  // }

  // onDeleted(e) {
  //   console.log('Deleted !');
  // }
  //
  // _onEditStart() {
  //   console.log('Edit is starting !');
  // }
  //
  // _onEditStop(e) {
  //   console.log('Edit is stopping !');
  // }
  //
  // _onDeleteStart(e) {
  //   console.log('Delete is starting !');
  // }
  //
  //  open_onDeleteStop(e) {
  //   console.log('Delete is stopping !');
  // }

  toggleDraggable = () => {
    this.setState({
      draggable: !this.state.draggable
    });
  }

  updatePosition = () => {
    const { lat, lng } = this.refs.marker.leafletElement.getLatLng();

    DataProvider(UPDATE, 'cubes/location', {
      id: this.props.record.id,
      data: {
        lat: lat,
        lng: lng
      }
    })
    .then(() => {
      this.props.dispatch(showNotification(`${this.props.record.deviceID} - Location updated`));
      this.setState({
        marker: { lat, lng }
      });
    })
    .catch((e) => {
      console.error(e);
      this.props.dispatch(showNotification('Cannot update location', 'warning'));
    });

  }

  render() {
    const { record } = this.props;
    const { geo } = this.state;
    const markerPosition = [this.state.marker.lat, this.state.marker.lng];

    const marker = (record.location ?
      <Marker
        draggable={this.state.draggable}
        onDragend={this.updatePosition}
        position={markerPosition}
        icon={blueMarker}
        ref="marker"
      >
        <Popup minWidth={200} open>
          <span>
            {
              record.online
              ?
              <div style={{color: green500, textAlign: 'center' }}>ONLINE</div>
              :
              <div style={{color: red500, textAlign: 'center' }}>OFFLINE</div>
            }
            Device: <b>{record.deviceID}</b>
            <br/>
            Customer: {record.customerID}
            <br/>
            <a href={`#/commands?filter={"q":"${record.deviceID}"}&page=1&perPage=10&sort=created&order=DESC`}>Commands</a>
            <br/>
            <a href={`#/reports?filter={"q":"${record.deviceID}"}&page=1&perPage=10&sort=reportID&order=ASC`}>Reports</a>
            <br/>
            <br/>
            <i>NOTE: Hold and move to change location</i>
          </span>
        </Popup>
      </Marker>
      :
      null
    );

    return (
      <Map
        animate={true}
        length={4}
        center={markerPosition}
        minZoom={2}
        maxZoom={19}
        zoom={15}
        ref="map"
      >
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
          {marker}
        </LayersControl>
        {/* <FeatureGroup>
          <EditControl
            onMounted={this.onMounted}
            onCreated={this.onCreated}
            onEdited={this.onEdited}
            onDeleted={this.onDeleted}

            onEditStart={this._onEditStart}
            onEditStop={this._onEditStop}

            onDeleteStart={this._onDeleteStart}
            onDeleteStop={this._onDeleteStop}
            draw={{
              polyline: true,
              polygon: false,
              rectangle: false,
              circle: false,
              marker: false
            }}
          />
          { geo &&
            _.map(geo.features, (feature, index) => {
              console.log('iter - ', feature, index);
              if (feature.geometry.type === 'Polygon') {
                return (
                  <Polygon key={index} positions={L.GeoJSON.coordsToLatLngs(feature.geometry.coordinates)} />
                );
              }
              if (feature.geometry.type === 'LineString') {
                return(
                  <Polyline key={index} positions={L.GeoJSON.coordsToLatLngs(feature.geometry.coordinates)} />
                )
              }
            })
          }
        </FeatureGroup> */}
      </Map>
    );
  }
}

//export default CubeMap;
export default connect(null, (dispatch) => ({dispatch}))(CubeMap);

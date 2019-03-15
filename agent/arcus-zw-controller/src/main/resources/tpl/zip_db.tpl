#extends spy_tools content_spy
<div class="btn-group pt-2 pb-3" role="group">
	<a href="/spy?p=zip" class="btn btn-outline-primary" role="button">Summary</a>
	<button type="button" class="btn btn-primary">Database</button>
	<a href="/spy?p=zipmsg" class="btn btn-outline-primary" role="button">Messages</a>
	<a href="/spy?p=ziptools" class="btn btn-outline-primary" role="button">Tools</a>
</div>

<div class="row">
	<div class="col">
	    <div class="card">
			<div class="card-header">
				<span class="lead">Hub Zip Database</span>
			</div> <!-- card-header -->
			
			<div class="card-body">
				<div id="zipdatabase">
					<h2>Configuration</h2>
					<table border="1" cellpadding="2">
						<tr><th>Key</th><th>Value</th></tr>
						<tr v-for="pair in configs">
							<td>{{ pair.key }}</td><td>{{ pair.value }}</td>
						</tr>
					</table>
					<p />
					<h2>Nodes</h2>
					<table border="1" cellpadding="2">
						<tr>
							<th>Node Id</th>
							<th>Basic</th>
							<th>Generic</th>
							<th>Specific</th>
							<th>Man Id</th>
							<th>Type Id</th>
							<th>Product Id</th>
							<th>Online</th>
							<th>Offline Timeout</th>
							<th>Command Classes</th>
						</tr>
						<tr v-for="node in nodes">
							<td>{{ node.nodeId }}</td>
							<td>{{ node.basicDeviceType }}</td>
							<td>{{ node.genericDeviceType }}</td>
							<td>{{ node.specificDeviceType }}</td>
							<td>{{ node.manufacturerId }}</td>
							<td>{{ node.productTypeId }}</td>
							<td>{{ node.productId }}</td>
							<td>{{ node.online }}</td>
							<td>{{ node.offlineTimeout}}</td>
						    <td>{{ node.cmdClassSet}}</td>
						</tr>
					</table>
				</div>
		    </div> <!-- card-body -->
	   </div> <!-- card -->
    </div> <!-- col -->
</div> <!-- row -->

<script>
  new Vue({
    el: '#zipdatabase',
    data: {
      configs: "",
      nodes: ""
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=getzipdb').then(response => {
          this.configs = response.data.configs;
          this.nodes = response.data.nodes;
        });
      }
    },
    created: function () {
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 20000);
    }  
  });
</script>

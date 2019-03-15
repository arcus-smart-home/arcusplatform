#extends spy_tools content_spy
<div class="btn-group pt-2 pb-3" role="group">
	<button type="button"     class="btn btn-primary">Summary</button>
	<a href="/spy?p=zipdb"    class="btn btn-outline-primary" role="button">Database</a>
	<a href="/spy?p=zipmsg"   class="btn btn-outline-primary" role="button">Messages</a>
	<a href="/spy?p=ziptools" class="btn btn-outline-primary" role="button">Tools</a>
</div>

<div class="row">
	<div class="col">
	    <div class="card">
			<div class="card-header">
				<span class="lead">In-Memory Node Data</span>
			</div> <!-- card-header -->
			
			<div class="card-body">
				<div id="nodesapp">
				  <pre class="green-console px-2">
{{ loadnodes }}
                  </pre>
				</div>
		    </div> <!-- card-body -->
	   </div> <!-- card -->
    </div> <!-- col -->
</div> <!-- row -->

<script>
  new Vue({
    el: '#nodesapp',
    data: {
      loadnodes: ""
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=getzipnodes').then(response => {
          this.loadnodes = response.data.data;
        });
      }
    },
    created: function () {
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 2000);
    }  
  });
</script>

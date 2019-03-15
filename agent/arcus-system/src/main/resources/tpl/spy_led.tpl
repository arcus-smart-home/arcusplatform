#extends spy_tools content_spy
<div class="mb-3">
  <span class="lead">LED Status</span>
</div>

<div class="row">
  <div class="col-md-4">
  	<div class="card">
      <div class="card-header">
      	LED Status
      </div>
      <div class="card-body">
      	<div id="ledstatus">
      		{{ ledstate }}
      	</div>
      </div>
  	</div> <!-- card -->
  </div> <!-- col -->
</div> <!-- row -->

<script>
  new Vue({
    el: '#ledstatus',
    data: {
      ledstate: ""
    },
    methods: {
      loadData() {
        this.$http.get('/spy/api?p=getled').then(response => {
          this.ledstate = response.data.data;
        });
      }
    },
    created: function () {
      console.log("Created Called!");
      this.loadData();
      
      setInterval(function() {
        this.loadData();
      }.bind(this), 500);
    }  
  });
</script>